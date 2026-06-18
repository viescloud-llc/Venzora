package com.viescloud.llc.venzora.model.checkout;

import com.viescloud.llc.venzora.model.product.OrderFulfillment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned from {@code POST /api/v1/orders/checkout}. The frontend redirects the
 * buyer to {@link #approveUrl}; on return it calls
 * {@code POST /api/v1/orders/{orderFulfillment.id}/complete}.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CheckoutStartResponse {

    private OrderFulfillment orderFulfillment;
    private String approveUrl;
}
