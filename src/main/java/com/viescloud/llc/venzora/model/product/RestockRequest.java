package com.viescloud.llc.venzora.model.product;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body for {@code POST /api/v1/orders/{id}/restock}: which line items to put
 * back into stock, and how many of each (defaults to the remaining
 * un-restocked quantity when omitted/null).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestockRequest {

    private List<Item> items;

    /** Free-text reason stamped on every RETURN movement (optional). */
    private String reason;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Item {
        private UUID orderFulfillmentItemId;
        /** Null = restock everything not yet restocked for this item. */
        private Integer quantity;
    }
}
