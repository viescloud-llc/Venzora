package com.viescloud.llc.venzora.service.checkout;

import java.time.Instant;
import java.util.HashMap;
import java.util.Objects;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.viescloud.eco.viesspringutils.auto.model.checkout.CheckoutOrderStatusChangedEvent;
import com.viescloud.llc.venzora.dao.product.OrderFulfillmentDao;
import com.viescloud.llc.venzora.dao.product.ProductVariantDao;
import com.viescloud.llc.venzora.model.product.OrderFulfillment;
import com.viescloud.llc.venzora.model.product.OrderFulfillmentItem;
import com.viescloud.llc.venzora.model.product.ProductVariant;
import com.viescloud.llc.venzora.model.product.type.FulfillmentStatus;
import com.viescloud.llc.venzora.service.product.StockMovementService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Webhook → fulfillment bridge (intent § 11, checkout.md § 7). The library's
 * checkout module publishes {@link CheckoutOrderStatusChangedEvent} on every
 * payment-side status transition — capture, refund, cancel, and every
 * webhook-driven change. This listener finds the matching
 * {@link OrderFulfillment} via {@code checkoutOrderId} and keeps its status in
 * step, so PayPal-dashboard refunds and webhook-confirmed captures no longer
 * need an admin to flip the order by hand.
 *
 * <p><b>Transitions applied</b> (anything else is left alone — fulfillment
 * states like SHIPPED/DELIVERED are warehouse facts the payment side must not
 * regress):
 * <ul>
 *   <li>{@code CAPTURED}  → {@code PENDING → PROCESSING}, plus the same stock
 *       decrement {@code complete()} performs (deduped via the
 *       {@code checkout.stockDecremented} metadata flag, so whichever of the
 *       webhook or the buyer's complete() call lands first does the work and
 *       the other is a no-op).</li>
 *   <li>{@code REFUNDED}  → {@code REFUNDED} (from any state).</li>
 *   <li>{@code PARTIALLY_REFUNDED} → {@code PARTIALLY_REFUNDED} (unless
 *       already fully {@code REFUNDED}).</li>
 *   <li>{@code CANCELLED} / {@code FAILED} → same-named fulfillment status,
 *       but only while the fulfillment is still {@code PENDING}.</li>
 * </ul>
 *
 * <p>Stock is <em>not</em> restocked on refunds — physical goods come back
 * through the returns flow, which has its own lifecycle.
 *
 * <p>The event is published synchronously inside the transaction that changed
 * the payment status (webhook thread, or the buyer's capture call), and an
 * exception thrown here would roll that transaction back — so every branch is
 * guarded: on any failure we log and leave the fulfillment untouched rather
 * than void a real payment event. PayPal retries webhooks, so a transient
 * failure heals itself.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CheckoutFulfillmentListener {

    private final OrderFulfillmentDao fulfillmentDao;
    private final ProductVariantDao variantDao;
    private final StockMovementService stockMovementService;

    static final String STOCK_DECREMENTED_FLAG = "checkout.stockDecremented";

    @EventListener
    public void onCheckoutOrderStatusChanged(CheckoutOrderStatusChangedEvent event) {
        try {
            handle(event);
        } catch (Exception e) {
            log.error("Fulfillment sync failed for checkout order {} ({} -> {}, trigger {}): {}",
                    event.getOrder() != null ? event.getOrder().getId() : null,
                    event.getPreviousStatus(), event.getNewStatus(), event.getTrigger(), e.getMessage(), e);
        }
    }

    private void handle(CheckoutOrderStatusChangedEvent event) {
        if (event.getOrder() == null || event.getOrder().getId() == null || event.getNewStatus() == null) {
            return;
        }

        OrderFulfillment fulfillment = fulfillmentDao.findByCheckoutOrderId(event.getOrder().getId()).orElse(null);
        if (fulfillment == null) {
            // Checkout orders can legitimately exist without a fulfillment
            // (direct /checkout/orders API use); nothing to sync.
            log.debug("No fulfillment linked to checkout order {}; skipping", event.getOrder().getId());
            return;
        }

        FulfillmentStatus current = fulfillment.getStatus();
        switch (event.getNewStatus()) {
            case CAPTURED -> {
                if (current != FulfillmentStatus.PENDING) return;
                decrementStockOnce(fulfillment);
                transition(fulfillment, current, FulfillmentStatus.PROCESSING, event);
            }
            case REFUNDED -> {
                if (current == FulfillmentStatus.REFUNDED) return;
                transition(fulfillment, current, FulfillmentStatus.REFUNDED, event);
            }
            case PARTIALLY_REFUNDED -> {
                if (current == FulfillmentStatus.REFUNDED || current == FulfillmentStatus.PARTIALLY_REFUNDED) return;
                transition(fulfillment, current, FulfillmentStatus.PARTIALLY_REFUNDED, event);
            }
            case CANCELLED -> {
                if (current != FulfillmentStatus.PENDING) return;
                transition(fulfillment, current, FulfillmentStatus.CANCELLED, event);
            }
            case FAILED -> {
                if (current != FulfillmentStatus.PENDING) return;
                transition(fulfillment, current, FulfillmentStatus.FAILED, event);
            }
            default -> { /* CREATED / PENDING_APPROVAL / APPROVED carry no fulfillment meaning */ }
        }
    }

    /**
     * Same sale-recording {@code CheckoutOrchestratorService.complete()} performs
     * (via {@code StockMovementService.recordCheckoutSale} — one SALE ledger row
     * per item whose creation also moves the variant's stock, BE-8), guarded by
     * the shared metadata flag so it happens exactly once no matter which path
     * (webhook or complete) runs first.
     *
     * <p>Stock is PRE-CHECKED here instead of letting the movement post throw:
     * a RuntimeException from the joined {@code @Transactional} post() would
     * mark the surrounding webhook/capture transaction rollback-only even if
     * caught — and a payment event must never be voided over a stock race. A
     * would-be-negative item is logged and skipped; the discrepancy is visible
     * because its SALE row is missing from the ledger.
     */
    private void decrementStockOnce(OrderFulfillment fulfillment) {
        if (fulfillment.getMetadata() == null) {
            fulfillment.setMetadata(new HashMap<>());
        }
        if (Objects.equals(fulfillment.getMetadata().get(STOCK_DECREMENTED_FLAG), "true")) {
            return;
        }
        for (OrderFulfillmentItem item : fulfillment.getItems()) {
            ProductVariant pv = item.getProductVariant();
            if (pv == null || pv.getId() == null) continue;
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            long current = variantDao.findById(pv.getId())
                    .map(v -> v.getStockQuantity() == null ? 0L : v.getStockQuantity())
                    .orElse(0L);
            if (current - quantity < 0) {
                log.error("Stock would go negative for variant {} on order {}; skipping its SALE movement (current {}, qty {})",
                        pv.getSku(), fulfillment.getOrderNumber(), current, quantity);
                continue;
            }
            stockMovementService.recordCheckoutSale(
                    pv.getId(), quantity, fulfillment.getUserId(), fulfillment.getOrderNumber());
        }
        fulfillment.getMetadata().put(STOCK_DECREMENTED_FLAG, "true");
    }

    private void transition(OrderFulfillment fulfillment, FulfillmentStatus from, FulfillmentStatus to,
                            CheckoutOrderStatusChangedEvent event) {
        fulfillment.setStatus(to);
        if (fulfillment.getMetadata() == null) {
            fulfillment.setMetadata(new HashMap<>());
        }
        fulfillment.getMetadata().put("checkout.lastEvent",
                (event.getTrigger() == null ? "unknown" : event.getTrigger()) + " @ " + Instant.now());
        if (to == FulfillmentStatus.PROCESSING) {
            fulfillment.getMetadata().putIfAbsent("checkout.capturedAt", Instant.now().toString());
        }
        fulfillmentDao.save(fulfillment);
        log.info("Fulfillment {} {} -> {} (checkout {} via {})",
                fulfillment.getOrderNumber(), from, to, event.getNewStatus(), event.getTrigger());
    }
}
