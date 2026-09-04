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
    /** Subtotal of the lines the discount applies to (whole cart unless the discount has product matchers). */
    private BigDecimal eligibleSubtotal;

    public static DiscountValidationResponse ok(BigDecimal discountAmount) {
        return new DiscountValidationResponse(true, discountAmount, null, null);
    }

    public static DiscountValidationResponse ok(BigDecimal discountAmount, BigDecimal eligibleSubtotal) {
        return new DiscountValidationResponse(true, discountAmount, null, eligibleSubtotal);
    }

    public static DiscountValidationResponse rejected(String reason) {
        return new DiscountValidationResponse(false, null, reason, null);
    }
}
