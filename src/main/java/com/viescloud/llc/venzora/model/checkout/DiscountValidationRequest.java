package com.viescloud.llc.venzora.model.checkout;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for {@code POST /api/v1/discounts/validate}. The buyer's id is taken from
 * the {@code user_id} header, not this body.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiscountValidationRequest {

    private String code;
    private UUID cartId;
}
