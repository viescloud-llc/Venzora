package com.viescloud.llc.venzora.model.checkout;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of {@code TaxCalculator.calculate(...)}. When no TaxRule matches, all
 * fields are zero / null and {@link #tax} is {@code BigDecimal.ZERO}.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaxCalculation {

    private BigDecimal tax;
    private BigDecimal rate;            // percentage applied, e.g. "8.00"; ZERO when no rule matches
    private UUID appliedRuleId;         // null when no rule matched
    private String appliedRuleName;     // null when no rule matched

    public static TaxCalculation zero() {
        return new TaxCalculation(BigDecimal.ZERO, BigDecimal.ZERO, null, null);
    }
}
