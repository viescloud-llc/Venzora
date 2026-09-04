package com.viescloud.llc.venzora.service.product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.viescloud.llc.venzora.dao.product.CategoryDao;
import com.viescloud.llc.venzora.dao.product.TaxRuleDao;
import com.viescloud.llc.venzora.model.address.Address;
import com.viescloud.llc.venzora.model.checkout.TaxCalculation;
import com.viescloud.llc.venzora.model.product.Cart;
import com.viescloud.llc.venzora.model.product.CartItem;
import com.viescloud.llc.venzora.model.product.Category;
import com.viescloud.llc.venzora.model.product.ProductVariant;
import com.viescloud.llc.venzora.model.product.TaxRule;

/**
 * Tax is computed PER LINE ITEM: every line picks the most specific active
 * {@link TaxRule} whose location matchers fit the shipping address (country /
 * state accept their alias lists; district is optional) AND whose product
 * matchers fit the sold product (tags, categories incl. ancestors, attribute
 * definitions — empty matcher = any). Ties on specificity go to priority.
 *
 * <p>The order discount is prorated across lines (rounding remainder on the
 * last line so the taxable sum equals subtotal − discount exactly), each line's
 * tax is rounded HALF_UP to 2 dp, and the order-level result carries the
 * per-line breakdown. Matching is in-memory over the active rules (hundreds,
 * not thousands, per tenant).
 */
@Service
public class TaxCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final TaxRuleDao taxRuleDao;
    private final CategoryDao categoryDao;

    public TaxCalculator(TaxRuleDao taxRuleDao, CategoryDao categoryDao) {
        this.taxRuleDao = taxRuleDao;
        this.categoryDao = categoryDao;
    }

    /**
     * Order-level calculation with no product context (kept for callers that
     * only know an amount): rules WITH product matchers are excluded since they
     * cannot be evaluated without a product.
     */
    public TaxCalculation calculate(BigDecimal subtotal, Address shippingAddress) {
        if (subtotal == null || subtotal.signum() <= 0) {
            return TaxCalculation.zero();
        }
        Optional<TaxRule> match = taxRuleDao.findAllByActiveTrue().stream()
                .filter(r -> !r.hasProductMatchers() && r.matchesLocation(shippingAddress))
                .max(ruleOrder());
        if (match.isEmpty()) {
            return TaxCalculation.zero();
        }
        TaxRule rule = match.get();
        BigDecimal tax = subtotal.multiply(rule.getRate()).divide(HUNDRED, 2, ROUNDING);
        return new TaxCalculation(tax, rule.getRate(), rule.getId(), rule.getName());
    }

    /** Per-line calculation for a cart — the discount applies to every line. */
    public TaxCalculation calculateForCart(Cart cart, BigDecimal discountAmount, Address shippingAddress) {
        return calculateForCart(cart, discountAmount, shippingAddress, null);
    }

    /**
     * Per-line calculation — the checkout path. {@code eligibleVariantIds}
     * (null = every line) restricts which lines the discount is prorated over:
     * a product-scoped discount must not reduce the taxable amount of lines it
     * never applied to.
     */
    public TaxCalculation calculateForCart(Cart cart, BigDecimal discountAmount, Address shippingAddress,
                                           Set<UUID> eligibleVariantIds) {
        List<CartItem> items = cart == null || cart.getItems() == null ? List.of() : cart.getItems();
        if (items.isEmpty()) {
            return TaxCalculation.zero();
        }
        BigDecimal subtotal = items.stream()
                .map(TaxCalculator::lineSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (subtotal.signum() <= 0) {
            return TaxCalculation.zero();
        }

        List<TaxRule> active = taxRuleDao.findAllByActiveTrue();
        Map<UUID, Category> categoriesById = ProductMatching.categoriesById(categoryDao);

        // Lines the discount is spread over (all, or only the eligible ones) and their subtotal.
        List<Boolean> eligible = new ArrayList<>();
        BigDecimal eligibleSum = BigDecimal.ZERO;
        int lastEligible = -1;
        for (int i = 0; i < items.size(); i++) {
            ProductVariant v = items.get(i).getProductVariant();
            boolean el = eligibleVariantIds == null || (v != null && v.getId() != null && eligibleVariantIds.contains(v.getId()));
            eligible.add(el);
            if (el) { eligibleSum = eligibleSum.add(lineSubtotal(items.get(i))); lastEligible = i; }
        }
        BigDecimal discount = discountAmount == null || discountAmount.signum() < 0 || eligibleSum.signum() <= 0
                ? BigDecimal.ZERO : discountAmount.min(eligibleSum);
        BigDecimal discountAssigned = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalTaxable = BigDecimal.ZERO;
        List<TaxCalculation.TaxLine> lines = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            CartItem item = items.get(i);
            BigDecimal line = lineSubtotal(item);
            // Prorate the discount over eligible lines; the last eligible line absorbs the rounding remainder.
            BigDecimal share;
            if (!eligible.get(i)) share = BigDecimal.ZERO;
            else if (i == lastEligible) share = discount.subtract(discountAssigned);
            else share = discount.multiply(line).divide(eligibleSum, 2, ROUNDING);
            discountAssigned = discountAssigned.add(share);
            BigDecimal taxable = line.subtract(share).max(BigDecimal.ZERO);

            ProductVariant variant = item.getProductVariant();
            Optional<TaxRule> best = pickRuleForProduct(active, shippingAddress, variant, categoriesById);
            BigDecimal rate = best.map(TaxRule::getRate).orElse(BigDecimal.ZERO);
            BigDecimal tax = taxable.multiply(rate).divide(HUNDRED, 2, ROUNDING);

            lines.add(new TaxCalculation.TaxLine(
                    variant == null ? null : variant.getSku(), taxable, rate,
                    best.map(TaxRule::getId).orElse(null), best.map(TaxRule::getName).orElse(null), tax));
            totalTax = totalTax.add(tax);
            totalTaxable = totalTaxable.add(taxable);
        }

        Set<UUID> distinctRules = lines.stream().map(TaxCalculation.TaxLine::getRuleId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toCollection(HashSet::new));
        TaxCalculation result = new TaxCalculation();
        result.setTax(totalTax);
        result.setLines(lines);
        if (distinctRules.size() == 1) {
            TaxCalculation.TaxLine any = lines.stream().filter(l -> l.getRuleId() != null).findFirst().orElseThrow();
            result.setRate(any.getRate());
            result.setAppliedRuleId(any.getRuleId());
            result.setAppliedRuleName(any.getRuleName());
        } else if (distinctRules.size() > 1) {
            result.setRate(totalTaxable.signum() > 0
                    ? totalTax.multiply(HUNDRED).divide(totalTaxable, 2, ROUNDING) : BigDecimal.ZERO);
            result.setAppliedRuleId(null);
            result.setAppliedRuleName("mixed: " + lines.stream().map(TaxCalculation.TaxLine::getRuleName)
                    .filter(java.util.Objects::nonNull).distinct().collect(Collectors.joining(", ")));
        } else {
            result.setRate(BigDecimal.ZERO);
        }
        return result;
    }

    private Optional<TaxRule> pickRuleForProduct(List<TaxRule> active, Address address, ProductVariant variant,
                                                 Map<UUID, Category> categoriesById) {
        ProductMatching.ProductContext ctx = ProductMatching.contextFor(variant, categoriesById);
        return active.stream()
                .filter(r -> r.matchesLocation(address))
                .filter(r -> r.matchesProduct(ctx))
                .max(ruleOrder());
    }

    /** Kept for callers/tests: the category plus every ancestor. */
    static Set<UUID> categoryChain(Category category, Map<UUID, Category> categoriesById) {
        return ProductMatching.categoryChain(category, categoriesById);
    }

    private static Comparator<TaxRule> ruleOrder() {
        return Comparator.comparingInt(TaxRule::specificity)
                .thenComparingInt(r -> r.getPriority() == null ? 0 : r.getPriority());
    }

    private static BigDecimal lineSubtotal(CartItem item) {
        if (item == null || item.getPriceAtTime() == null || item.getQuantity() == null) return BigDecimal.ZERO;
        return item.getPriceAtTime().multiply(BigDecimal.valueOf(item.getQuantity()));
    }
}
