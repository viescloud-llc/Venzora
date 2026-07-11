package com.viescloud.llc.venzora.model.checkout;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of a non-destructive discount check. Returned with HTTP 200 even when
 * {@link #valid} is false so the frontend can present a clean reason without
 * relying on exception handling.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiscountValidationResponse {

    private boolean valid;
    private BigDecimal discountAmount;
    private String reason;

    public static DiscountValidationResponse ok(BigDecimal discountAmount) {
        return new DiscountValidationResponse(true, discountAmount, null);
    }

    public static DiscountValidationResponse rejected(String reason) {
        return new DiscountValidationResponse(false, null, reason);
    }
}
