package com.viescloud.llc.venzora.model.checkout;

import java.util.UUID;

import com.viescloud.llc.venzora.model.address.Address;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for {@code POST /api/v1/orders/checkout}. The buyer's id is taken from
 * the {@code user_id} header, not this body.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CheckoutStartRequest {

    private UUID cartId;
    private Address shippingAddress;
    private Address billingAddress;
    private String discountCode;
    private String provider;
    private String returnUrl;
    private String cancelUrl;
}
