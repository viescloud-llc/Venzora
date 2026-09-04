package com.viescloud.llc.venzora.service.checkout;

import java.time.Instant;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.viescloud.llc.venzora.dao.product.OrderFulfillmentDao;
import com.viescloud.llc.venzora.model.product.OrderFulfillment;
import com.viescloud.llc.venzora.model.product.OrderFulfillmentItem;
import com.viescloud.llc.venzora.model.product.RestockRequest;
import com.viescloud.llc.venzora.model.product.type.FulfillmentStatus;
import com.viescloud.llc.venzora.service.product.StockMovementService;

import lombok.RequiredArgsConstructor;

/**
 * Puts refunded/returned goods back into stock, item by item, through the
 * stock ledger (one {@code RETURN} movement per item — balance and audit log
 * move together). Progress is tracked on the order's metadata bag
 * ({@code restock.<itemId>} = cumulative restocked qty, {@code restock.lastAt})
 * so a second call can only restock what is still outstanding — never more
 * than was sold.
 *
 * <p>Guards: the order must be in a refund/return family status, and stock must
 * actually have LEFT for this order ({@code checkout.stockDecremented} flag) —
 * a cancelled-before-capture order never decremented anything, so restocking
 * it would inflate inventory.
 */
@Service
@RequiredArgsConstructor
public class OrderRestockService {

    static final Set<FulfillmentStatus> RESTOCKABLE = Set.of(
            FulfillmentStatus.REFUNDED, FulfillmentStatus.PARTIALLY_REFUNDED,
            FulfillmentStatus.RETURNED, FulfillmentStatus.CANCELLED);

    private final OrderFulfillmentDao fulfillmentDao;
    private final StockMovementService stockMovementService;

    @Transactional
    public OrderFulfillment restock(UUID orderId, RestockRequest request, UUID actorUserId) {
        OrderFulfillment order = fulfillmentDao.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + orderId));

        if (!RESTOCKABLE.contains(order.getStatus())) {
            throw bad("Order " + order.getOrderNumber() + " is " + order.getStatus()
                    + " — restock is only allowed for REFUNDED / PARTIALLY_REFUNDED / RETURNED / CANCELLED orders");
        }
        if (order.getMetadata() == null) {
            order.setMetadata(new HashMap<>());
        }
        if (!"true".equals(order.getMetadata().get(CheckoutFulfillmentListener.STOCK_DECREMENTED_FLAG))) {
            throw bad("Stock never left for order " + order.getOrderNumber()
                    + " (payment was not captured) — nothing to restock");
        }
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw bad("items is required — which line items to restock");
        }

        int restockedTotal = 0;
        for (RestockRequest.Item req : request.getItems()) {
            if (req == null || req.getOrderFulfillmentItemId() == null) {
                throw bad("every item needs an orderFulfillmentItemId");
            }
            OrderFulfillmentItem item = order.getItems().stream()
                    .filter(i -> Objects.equals(i.getId(), req.getOrderFulfillmentItemId()))
                    .findFirst()
                    .orElseThrow(() -> bad("item " + req.getOrderFulfillmentItemId() + " does not belong to order " + order.getOrderNumber()));

            int sold = item.getQuantity() == null ? 0 : item.getQuantity();
            int already = parseInt(order.getMetadata().get(metaKey(item)));
            int remaining = sold - already;
            int quantity = req.getQuantity() == null ? remaining : req.getQuantity();

            if (quantity <= 0) {
                if (req.getQuantity() == null) continue; // nothing left for this item — skip silently
                throw bad("quantity must be positive for item " + item.getLineItemSku());
            }
            if (quantity > remaining) {
                throw bad("Cannot restock " + quantity + " of " + item.getLineItemSku()
                        + ": sold " + sold + ", already restocked " + already + ", remaining " + remaining);
            }
            if (item.getProductVariant() == null || item.getProductVariant().getId() == null) {
                throw bad("item " + item.getLineItemSku() + " has no variant to restock into");
            }

            stockMovementService.recordRestock(item.getProductVariant().getId(), quantity, actorUserId,
                    order.getOrderNumber(), request.getReason());
            order.getMetadata().put(metaKey(item), String.valueOf(already + quantity));
            restockedTotal += quantity;
        }

        if (restockedTotal == 0) {
            throw bad("Nothing to restock — every requested item is already fully restocked");
        }
        order.getMetadata().put("restock.lastAt", Instant.now().toString());
        return fulfillmentDao.save(order);
    }

    static String metaKey(OrderFulfillmentItem item) {
        return "restock." + item.getId();
    }

    private static int parseInt(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static ResponseStatusException bad(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
