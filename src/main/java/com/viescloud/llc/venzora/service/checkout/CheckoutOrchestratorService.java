package com.viescloud.llc.venzora.service.checkout;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.viescloud.eco.viesspringutils.auto.model.checkout.CheckoutCreateOrderRequest;
import com.viescloud.eco.viesspringutils.auto.model.checkout.CheckoutLineItem;
import com.viescloud.eco.viesspringutils.auto.model.checkout.CheckoutOrder;
import com.viescloud.eco.viesspringutils.auto.service.checkout.CheckoutProviderRegistry;
import com.viescloud.llc.venzora.dao.product.CartDao;
import com.viescloud.llc.venzora.dao.product.DiscountDao;
import com.viescloud.llc.venzora.dao.product.OrderFulfillmentDao;
import com.viescloud.llc.venzora.dao.product.ProductVariantDao;
import com.viescloud.llc.venzora.model.checkout.CheckoutStartRequest;
import com.viescloud.llc.venzora.model.checkout.CheckoutStartResponse;
import com.viescloud.llc.venzora.model.product.Cart;
import com.viescloud.llc.venzora.model.product.CartItem;
import com.viescloud.llc.venzora.model.product.Discount;
import com.viescloud.llc.venzora.model.product.OrderFulfillment;
import com.viescloud.llc.venzora.model.product.OrderFulfillmentItem;
import com.viescloud.llc.venzora.model.product.ProductVariant;
import com.viescloud.llc.venzora.model.product.type.FulfillmentStatus;
import com.viescloud.llc.venzora.model.share_enum.Currency;

/**
 * Orchestrates the multi-entity checkout flow:
 *
 * <ol>
 *   <li>{@link #start} — validate cart + discount + stock, create OrderFulfillment,
 *       call the library to create a CheckoutOrder, link them, deactivate the cart.</li>
 *   <li>{@link #complete} — capture payment via the library, decrement stock,
 *       flip OrderFulfillment.status to PROCESSING.</li>
 * </ol>
 *
 * <p>The checkout module is conditional on PayPal credentials. When it is not
 * registered, both methods throw {@code 503 SERVICE_UNAVAILABLE}.
 */
@Service
public class CheckoutOrchestratorService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final CartDao cartDao;
    private final OrderFulfillmentDao fulfillmentDao;
    private final DiscountDao discountDao;
    private final ProductVariantDao variantDao;
    private final ObjectProvider<CheckoutProviderRegistry> checkoutProvider;

    public CheckoutOrchestratorService(CartDao cartDao,
                                       OrderFulfillmentDao fulfillmentDao,
                                       DiscountDao discountDao,
                                       ProductVariantDao variantDao,
                                       ObjectProvider<CheckoutProviderRegistry> checkoutProvider) {
        this.cartDao = cartDao;
        this.fulfillmentDao = fulfillmentDao;
        this.discountDao = discountDao;
        this.variantDao = variantDao;
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

        BigDecimal subtotal = cart.getItems().stream()
                .map(ci -> ci.getPriceAtTime().multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Discount discount = null;
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (req.getDiscountCode() != null && !req.getDiscountCode().isBlank()) {
            discount = discountDao.findByCode(req.getDiscountCode())
                    .orElseThrow(() -> bad("Discount code not found: " + req.getDiscountCode()));
            validateDiscount(discount, subtotal);
            discountAmount = computeDiscount(discount, subtotal);
        }

        BigDecimal tax = BigDecimal.ZERO;           // TODO: tax engine
        BigDecimal shippingCost = BigDecimal.ZERO;  // TODO: shipping calculator
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
        fulfillment.setSubtotal(subtotal);
        fulfillment.setTax(tax);
        fulfillment.setShippingCost(shippingCost);
        fulfillment.setDiscountAmount(discountAmount);
        fulfillment.setTotalAmount(total);
        fulfillment.setStatus(FulfillmentStatus.PENDING);
        fulfillment.setShippingAddress(req.getShippingAddress());
        fulfillment.setBillingAddress(req.getBillingAddress());

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
        return fulfillmentDao.save(fulfillment);
    }

    // ---- helpers ----

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

    private void validateDiscount(Discount d, BigDecimal subtotal) {
        if (!Boolean.TRUE.equals(d.getActive())) {
            throw bad("Discount is inactive: " + d.getCode());
        }
        if (d.getMaxUses() != null && d.getCurrentUses() >= d.getMaxUses()) {
            throw bad("Discount usage limit reached: " + d.getCode());
        }
        if (d.getMinimumOrderAmount() != null
                && subtotal.compareTo(d.getMinimumOrderAmount()) < 0) {
            throw bad("Order subtotal below minimum for discount " + d.getCode());
        }
        // validFrom / validTo comparison deferred until DateTime helpers are wired.
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
