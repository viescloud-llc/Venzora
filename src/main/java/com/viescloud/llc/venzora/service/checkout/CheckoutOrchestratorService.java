package com.viescloud.llc.venzora.service.checkout;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.viescloud.eco.viesspringutils.auto.model.checkout.CheckoutCreateOrderRequest;
import com.viescloud.eco.viesspringutils.auto.model.checkout.CheckoutLineItem;
import com.viescloud.eco.viesspringutils.auto.model.checkout.CheckoutOrder;
import com.viescloud.eco.viesspringutils.auto.service.checkout.CheckoutProviderRegistry;
import com.viescloud.eco.viesspringutils.util.DateTime;
import com.viescloud.llc.venzora.dao.product.CartDao;
import com.viescloud.llc.venzora.model.address.Address;
import com.viescloud.llc.venzora.dao.product.DiscountDao;
import com.viescloud.llc.venzora.dao.product.OrderFulfillmentDao;
import com.viescloud.llc.venzora.dao.product.ProductVariantDao;
import com.viescloud.llc.venzora.dao.product.ShippingRuleDao;
import com.viescloud.llc.venzora.model.checkout.CheckoutStartRequest;
import com.viescloud.llc.venzora.model.checkout.CheckoutStartResponse;
import com.viescloud.llc.venzora.model.checkout.DiscountValidationResponse;
import com.viescloud.llc.venzora.model.checkout.TaxCalculation;
import com.viescloud.llc.venzora.model.product.Cart;
import com.viescloud.llc.venzora.model.product.CartItem;
import com.viescloud.llc.venzora.model.product.Discount;
import com.viescloud.llc.venzora.model.product.OrderFulfillment;
import com.viescloud.llc.venzora.model.product.OrderFulfillmentItem;
import com.viescloud.llc.venzora.model.product.ProductVariant;
import com.viescloud.llc.venzora.model.product.ShippingRule;
import com.viescloud.llc.venzora.model.product.type.FulfillmentStatus;
import com.viescloud.llc.venzora.model.share_enum.Currency;
import com.viescloud.llc.venzora.service.product.TaxCalculator;

/**
 * Orchestrates the multi-entity checkout flow:
 *
 * <ol>
 *   <li>{@link #start} — validate cart + discount + stock, create OrderFulfillment,
 *       call the library to create a CheckoutOrder, link them, deactivate the cart.</li>
 *   <li>{@link #complete} — capture payment via the library, decrement stock,
 *       flip OrderFulfillment.status to PROCESSING.</li>
 *   <li>{@link #validateDiscountForCart} — non-destructive coupon preview for the
 *       frontend "Apply code" UX.</li>
 * </ol>
 *
 * <p>The checkout module is conditional on PayPal credentials. When it is not
 * registered, {@link #start} and {@link #complete} throw 503. Validation does not
 * require the checkout module.
 */
@Service
public class CheckoutOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutOrchestratorService.class);
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final CartDao cartDao;
    private final OrderFulfillmentDao fulfillmentDao;
    private final DiscountDao discountDao;
    private final ProductVariantDao variantDao;
    private final ShippingRuleDao shippingRuleDao;
    private final TaxCalculator taxCalculator;
    private final ObjectProvider<CheckoutProviderRegistry> checkoutProvider;

    public CheckoutOrchestratorService(CartDao cartDao,
                                       OrderFulfillmentDao fulfillmentDao,
                                       DiscountDao discountDao,
                                       ProductVariantDao variantDao,
                                       ShippingRuleDao shippingRuleDao,
                                       TaxCalculator taxCalculator,
                                       ObjectProvider<CheckoutProviderRegistry> checkoutProvider) {
        this.cartDao = cartDao;
        this.fulfillmentDao = fulfillmentDao;
        this.discountDao = discountDao;
        this.variantDao = variantDao;
        this.shippingRuleDao = shippingRuleDao;
        this.taxCalculator = taxCalculator;
        this.checkoutProvider = checkoutProvider;
    }

    @Transactional
    public CheckoutStartResponse start(CheckoutStartRequest req, UUID buyerId) {
        requireField(req.getCartId(), "cartId");
        requireField(req.getProvider(), "provider");
        requireField(req.getReturnUrl(), "returnUrl");
        requireField(req.getCancelUrl(), "cancelUrl");
        requireField(req.getShippingAddress(), "shippingAddress");
        requireField(req.getBillingAddress(), "billingAddress");

        Cart cart = cartDao.findById(req.getCartId())
                .orElseThrow(() -> notFound("Cart not found: " + req.getCartId()));
        if (!Objects.equals(cart.getUserId(), buyerId)) {
            throw forbidden("Cart does not belong to the authenticated user");
        }
        if (Boolean.FALSE.equals(cart.getActive())) {
            throw bad("Cart is not active");
        }
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw bad("Cart is empty");
        }

        Currency currency = singleCurrencyOf(cart);

        for (CartItem ci : cart.getItems()) {
            ProductVariant pv = ci.getProductVariant();
            Long stock = pv.getStockQuantity();
            if (stock == null || stock < ci.getQuantity()) {
                throw bad("Insufficient stock for variant " + pv.getSku());
            }
        }

        BigDecimal subtotal = sumLineItems(cart);

        Discount discount = null;
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (req.getDiscountCode() != null && !req.getDiscountCode().isBlank()) {
            discount = discountDao.findByCode(req.getDiscountCode())
                    .orElseThrow(() -> bad("Discount code not found: " + req.getDiscountCode()));
            String rejection = checkDiscount(discount, subtotal);
            if (rejection != null) {
                throw bad(rejection);
            }
            discountAmount = computeDiscount(discount, subtotal);
        }

        TaxCalculation taxCalc = taxCalculator.calculate(
                subtotal.subtract(discountAmount), req.getShippingAddress());
        BigDecimal tax = taxCalc.getTax();
        BigDecimal shippingCost = computeShipping(currency, subtotal);
        BigDecimal total = subtotal.subtract(discountAmount).add(tax).add(shippingCost);
        if (total.signum() < 0) {
            total = BigDecimal.ZERO;
        }

        List<CheckoutLineItem> lineItems = new ArrayList<>();
        for (CartItem ci : cart.getItems()) {
            ProductVariant pv = ci.getProductVariant();
            BigDecimal lineSubtotal = ci.getPriceAtTime().multiply(BigDecimal.valueOf(ci.getQuantity()));
            lineItems.add(CheckoutLineItem.builder()
                    .sku(pv.getSku())
                    .name(pv.getVariantName() != null ? pv.getVariantName() : pv.getSku())
                    .quantity(ci.getQuantity())
                    .unitPrice(ci.getPriceAtTime())
                    .subtotal(lineSubtotal)
                    .build());
        }

        CheckoutCreateOrderRequest checkoutReq = CheckoutCreateOrderRequest.builder()
                .currency(currency.name())
                .items(lineItems)
                .description("Venzora order from cart " + cart.getId())
                .returnUrl(req.getReturnUrl())
                .cancelUrl(req.getCancelUrl())
                .metadata(Map.of("cartId", cart.getId().toString()))
                .build();

        CheckoutOrder checkoutOrder = registry().orderService(req.getProvider())
                .createOrder(checkoutReq, buyerId);

        OrderFulfillment fulfillment = new OrderFulfillment();
        fulfillment.setOrderNumber(generateOrderNumber());
        fulfillment.setUserId(buyerId);
        fulfillment.setCheckoutOrderId(checkoutOrder.getId());
        fulfillment.setCurrency(currency);
        fulfillment.setSubtotal(subtotal);
        fulfillment.setTax(tax);
        fulfillment.setShippingCost(shippingCost);
        fulfillment.setDiscountAmount(discountAmount);
        fulfillment.setTotalAmount(total);
        fulfillment.setStatus(FulfillmentStatus.PENDING);
        fulfillment.setShippingAddress(req.getShippingAddress());
        fulfillment.setBillingAddress(req.getBillingAddress());
        fulfillment.setMetadata(buildStartMetadata(
                req, cart, checkoutOrder, currency,
                discount, discountAmount, taxCalc, shippingCost));

        List<OrderFulfillmentItem> items = new ArrayList<>();
        for (CartItem ci : cart.getItems()) {
            OrderFulfillmentItem ofi = new OrderFulfillmentItem();
            ofi.setOrderFulfillment(fulfillment);
            ofi.setProductVariant(ci.getProductVariant());
            ofi.setQuantity(ci.getQuantity());
            ofi.setUnitPrice(ci.getPriceAtTime());
            ofi.setTotalPrice(ci.getPriceAtTime().multiply(BigDecimal.valueOf(ci.getQuantity())));
            ofi.setLineItemSku(ci.getProductVariant().getSku());
            items.add(ofi);
        }
        fulfillment.setItems(items);
        fulfillment = fulfillmentDao.save(fulfillment);

        if (discount != null) {
            discount.setCurrentUses(discount.getCurrentUses() + 1);
            discountDao.save(discount);
        }

        cart.setActive(false);
        cartDao.save(cart);

        return new CheckoutStartResponse(fulfillment, checkoutOrder.getApproveUrl());
    }

    @Transactional
    public OrderFulfillment complete(UUID fulfillmentId, UUID buyerId) {
        OrderFulfillment fulfillment = fulfillmentDao.findById(fulfillmentId)
                .orElseThrow(() -> notFound("Order not found: " + fulfillmentId));
        if (!Objects.equals(fulfillment.getUserId(), buyerId)) {
            throw forbidden("Order does not belong to the authenticated user");
        }
        if (fulfillment.getCheckoutOrderId() == null) {
            throw bad("Order has no linked checkout");
        }
        if (fulfillment.getStatus() != FulfillmentStatus.PENDING) {
            throw bad("Order is not in PENDING state (current: " + fulfillment.getStatus() + ")");
        }

        // Provider can be derived from the CheckoutOrder via the registry once the
        // library exposes a per-order provider lookup. For now PayPal is the only
        // configured provider, so resolve directly.
        registry().orderService("paypal").captureOrder(fulfillment.getCheckoutOrderId());

        for (OrderFulfillmentItem item : fulfillment.getItems()) {
            ProductVariant pv = item.getProductVariant();
            long current = pv.getStockQuantity() == null ? 0L : pv.getStockQuantity();
            long updated = current - item.getQuantity();
            if (updated < 0) {
                throw bad("Stock went negative for variant " + pv.getSku() + " — race detected");
            }
            pv.setStockQuantity(updated);
            variantDao.save(pv);
        }

        fulfillment.setStatus(FulfillmentStatus.PROCESSING);
        if (fulfillment.getMetadata() == null) {
            fulfillment.setMetadata(new HashMap<>());
        }
        fulfillment.getMetadata().put("checkout.capturedAt", Instant.now().toString());
        return fulfillmentDao.save(fulfillment);
    }

    /**
     * Non-destructive coupon preview. Used by the frontend "Apply code" button so
     * the user can test a code without committing to the destructive checkout flow.
     * Returns HTTP 200 with {@code valid=false} when the code is rejected for any
     * business reason; throws 4xx only on transport-level problems (cart missing,
     * cart not owned).
     */
    @Transactional(readOnly = true)
    public DiscountValidationResponse validateDiscountForCart(String code, UUID cartId, UUID buyerId) {
        if (code == null || code.isBlank()) {
            return DiscountValidationResponse.rejected("Code is empty");
        }
        if (cartId == null) {
            throw bad("cartId is required");
        }

        Cart cart = cartDao.findById(cartId)
                .orElseThrow(() -> notFound("Cart not found: " + cartId));
        if (!Objects.equals(cart.getUserId(), buyerId)) {
            throw forbidden("Cart does not belong to the authenticated user");
        }
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            return DiscountValidationResponse.rejected("Cart is empty");
        }

        BigDecimal subtotal = sumLineItems(cart);

        Discount discount = discountDao.findByCode(code).orElse(null);
        if (discount == null) {
            return DiscountValidationResponse.rejected("Code not found");
        }

        String rejection = checkDiscount(discount, subtotal);
        if (rejection != null) {
            return DiscountValidationResponse.rejected(rejection);
        }

        return DiscountValidationResponse.ok(computeDiscount(discount, subtotal));
    }

    // ---- helpers ----

    /**
     * Snapshot bag written onto {@link OrderFulfillment#getMetadata()} at sale time.
     * System keys use dotted prefixes ({@code checkout.}, {@code tax.}, {@code discount.},
     * {@code shipping.}). Manager-added notes are conventionally prefixed {@code notes.}.
     */
    private Map<String, String> buildStartMetadata(CheckoutStartRequest req,
                                                    Cart cart,
                                                    CheckoutOrder checkoutOrder,
                                                    Currency currency,
                                                    Discount discount,
                                                    BigDecimal discountAmount,
                                                    TaxCalculation taxCalc,
                                                    BigDecimal shippingCost) {
        Map<String, String> meta = new HashMap<>();
        put(meta, "checkout.provider", req.getProvider());
        put(meta, "checkout.providerOrderId", checkoutOrder.getProviderOrderId());
        put(meta, "checkout.approveUrl", checkoutOrder.getApproveUrl());
        put(meta, "checkout.cartId", cart.getId() == null ? null : cart.getId().toString());
        put(meta, "checkout.currency", currency.name());

        if (discount != null) {
            put(meta, "discount.code", discount.getCode());
            put(meta, "discount.appliedAmount", discountAmount.toPlainString());
            put(meta, "discount.type", discount.getDiscountType().name());
            put(meta, "discount.ruleId", discount.getId() == null ? null : discount.getId().toString());
        }

        if (taxCalc != null && taxCalc.getAppliedRuleId() != null) {
            put(meta, "tax.ruleId", taxCalc.getAppliedRuleId().toString());
            put(meta, "tax.ruleName", taxCalc.getAppliedRuleName());
            put(meta, "tax.rate", taxCalc.getRate().toPlainString());
            put(meta, "tax.jurisdiction", formatJurisdiction(req.getShippingAddress()));
        } else {
            put(meta, "tax.rate", "0");
        }

        Optional<ShippingRule> shipRule = shippingRuleDao.findByCurrency(currency);
        if (shipRule.isPresent()) {
            ShippingRule rule = shipRule.get();
            put(meta, "shipping.ruleId", rule.getId() == null ? null : rule.getId().toString());
            put(meta, "shipping.flatFee", rule.getFlatFee() == null ? null : rule.getFlatFee().toPlainString());
            if (shippingCost.signum() == 0 && rule.getFreeAboveAmount() != null) {
                put(meta, "shipping.freeShipping", "true");
            }
        }

        return meta;
    }

    private static void put(Map<String, String> meta, String key, String value) {
        if (value != null && !value.isBlank()) {
            meta.put(key, value);
        }
    }

    private static String formatJurisdiction(Address addr) {
        if (addr == null) return "";
        return Stream.of(addr.getCountry(), addr.getState(), addr.getCity(), addr.getPostalCode())
                .filter(s -> s != null && !s.isBlank())
                .reduce((a, b) -> a + "/" + b)
                .orElse("");
    }

    private BigDecimal sumLineItems(Cart cart) {
        return cart.getItems().stream()
                .map(ci -> ci.getPriceAtTime().multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Currency singleCurrencyOf(Cart cart) {
        Currency first = null;
        for (CartItem ci : cart.getItems()) {
            Currency c = ci.getProductVariant().getProduct().getCurrency();
            if (first == null) {
                first = c;
            } else if (c != first) {
                throw bad("Cart has mixed currencies: " + first + " and " + c);
            }
        }
        if (first == null) {
            throw bad("Cart has no items with a currency");
        }
        return first;
    }

    /**
     * Returns null when the discount is valid, otherwise a human-readable rejection
     * reason. Used by both {@link #start} (which throws 400) and
     * {@link #validateDiscountForCart} (which returns the reason as JSON).
     */
    private String checkDiscount(Discount d, BigDecimal subtotal) {
        if (!Boolean.TRUE.equals(d.getActive())) {
            return "Discount is inactive";
        }
        DateTime now = DateTime.now();
        if (d.getValidFrom() != null && now.isBefore(d.getValidFrom())) {
            return "Discount not yet active (valid from " + d.getValidFrom().getDateTime() + ")";
        }
        if (d.getValidTo() != null && now.isAfter(d.getValidTo())) {
            return "Discount expired (valid to " + d.getValidTo().getDateTime() + ")";
        }
        if (d.getMaxUses() != null && d.getCurrentUses() >= d.getMaxUses()) {
            return "Discount usage limit reached";
        }
        if (d.getMinimumOrderAmount() != null
                && subtotal.compareTo(d.getMinimumOrderAmount()) < 0) {
            return "Order subtotal below minimum (" + d.getMinimumOrderAmount() + ") for discount";
        }
        return null;
    }

    private BigDecimal computeDiscount(Discount d, BigDecimal subtotal) {
        BigDecimal raw;
        switch (d.getDiscountType()) {
            case PERCENTAGE -> raw = subtotal.multiply(d.getDiscountValue())
                                             .divide(HUNDRED, 2, RoundingMode.HALF_UP);
            case FIXED_AMOUNT -> raw = d.getDiscountValue();
            case FREE_SHIPPING, BUY_X_GET_Y -> raw = BigDecimal.ZERO;
            default -> raw = BigDecimal.ZERO;
        }
        if (d.getMaximumDiscountAmount() != null
                && raw.compareTo(d.getMaximumDiscountAmount()) > 0) {
            raw = d.getMaximumDiscountAmount();
        }
        return raw;
    }

    private BigDecimal computeShipping(Currency currency, BigDecimal subtotal) {
        Optional<ShippingRule> ruleOpt = shippingRuleDao.findByCurrency(currency);
        if (ruleOpt.isEmpty()) {
            log.warn("No ShippingRule configured for currency {} - treating shipping as zero", currency);
            return BigDecimal.ZERO;
        }
        ShippingRule rule = ruleOpt.get();
        if (!Boolean.TRUE.equals(rule.getActive())) {
            log.warn("ShippingRule for currency {} is inactive - treating shipping as zero", currency);
            return BigDecimal.ZERO;
        }
        if (rule.getFreeAboveAmount() != null
                && subtotal.compareTo(rule.getFreeAboveAmount()) >= 0) {
            return BigDecimal.ZERO;
        }
        return rule.getFlatFee();
    }

    private String generateOrderNumber() {
        return "VEN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private CheckoutProviderRegistry registry() {
        CheckoutProviderRegistry r = checkoutProvider.getIfAvailable();
        if (r == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Checkout module not configured. Set PAYPAL_CLIENT_ID and PAYPAL_CLIENT_SECRET.");
        }
        return r;
    }

    private static void requireField(Object value, String name) {
        if (value == null || (value instanceof String s && s.isBlank())) {
            throw bad(name + " is required");
        }
    }

    private static ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    private static ResponseStatusException notFound(String msg) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
    }

    private static ResponseStatusException forbidden(String msg) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, msg);
    }
}
