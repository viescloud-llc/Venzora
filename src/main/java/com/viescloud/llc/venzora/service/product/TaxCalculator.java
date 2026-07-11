package com.viescloud.llc.venzora.service.product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.viescloud.llc.venzora.dao.product.TaxRuleDao;
import com.viescloud.llc.venzora.model.address.Address;
import com.viescloud.llc.venzora.model.checkout.TaxCalculation;
import com.viescloud.llc.venzora.model.product.TaxRule;

/**
 * Picks the most specific matching {@link TaxRule} for a shipping address and
 * applies its rate to a subtotal. Matching is in-memory (load all active rules,
 * filter, sort) — fine for the ~hundreds-of-rules scale expected for a single
 * tenant; revisit if rule counts grow into the thousands.
 */
@Service
public class TaxCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final TaxRuleDao taxRuleDao;

    public TaxCalculator(TaxRuleDao taxRuleDao) {
        this.taxRuleDao = taxRuleDao;
    }

    public TaxCalculation calculate(BigDecimal subtotal, Address shippingAddress) {
        if (subtotal == null || subtotal.signum() <= 0) {
            return TaxCalculation.zero();
        }
        Optional<TaxRule> match = pickMatchingRule(shippingAddress);
        if (match.isEmpty()) {
            return TaxCalculation.zero();
        }
        TaxRule rule = match.get();
        BigDecimal tax = subtotal.multiply(rule.getRate())
                                  .divide(HUNDRED, 2, ROUNDING);
        return new TaxCalculation(tax, rule.getRate(), rule.getId(), rule.getName());
    }

    private Optional<TaxRule> pickMatchingRule(Address addr) {
        List<TaxRule> active = taxRuleDao.findAllByActiveTrue();
        return active.stream()
                .filter(r -> matches(r, addr))
                .max(Comparator.comparingInt(this::specificity)
                                .thenComparingInt(TaxRule::getPriority));
    }

    private boolean matches(TaxRule rule, Address addr) {
        if (addr == null) {
            // A null address only matches the all-null catch-all.
            return specificity(rule) == 0;
        }
        if (rule.getCountry() != null
                && !equalsIgnoreCase(rule.getCountry(), addr.getCountry())) {
            return false;
        }
        if (rule.getState() != null
                && !equalsIgnoreCase(rule.getState(), addr.getState())) {
            return false;
        }
        if (rule.getCity() != null
                && !equalsIgnoreCase(rule.getCity(), addr.getCity())) {
            return false;
        }
        if (rule.getPostalCode() != null
                && !rule.getPostalCode().equals(addr.getPostalCode())) {
            return false;
        }
        return true;
    }

    private int specificity(TaxRule rule) {
        int s = 0;
        if (rule.getCountry() != null) s++;
        if (rule.getState() != null) s++;
        if (rule.getCity() != null) s++;
        if (rule.getPostalCode() != null) s++;
        return s;
    }

    private static boolean equalsIgnoreCase(String a, String b) {
        if (a == null || b == null) return false;
        return a.trim().equalsIgnoreCase(b.trim());
    }
}
