package com.viescloud.llc.venzora.model.checkout;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
    private BigDecimal rate;            // percentage applied when ONE rule covered every line; effective rate when mixed; ZERO when none
    private UUID appliedRuleId;         // the single applied rule, null when none or mixed
    private String appliedRuleName;     // the single applied rule's name; "mixed" summary when several rules applied

    /** Per-line breakdown (product matchers make tax line-level). Empty for the zero result. */
    private List<TaxLine> lines = new ArrayList<>();

    public TaxCalculation(BigDecimal tax, BigDecimal rate, UUID appliedRuleId, String appliedRuleName) {
        this(tax, rate, appliedRuleId, appliedRuleName, new ArrayList<>());
    }

    public static TaxCalculation zero() {
        return new TaxCalculation(BigDecimal.ZERO, BigDecimal.ZERO, null, null);
    }

    /** True when more than one distinct rule was applied across the lines. */
    public boolean isMixed() {
        return lines != null && lines.stream().map(TaxLine::getRuleId).filter(java.util.Objects::nonNull).distinct().count() > 1;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TaxLine {
        private String sku;
        private BigDecimal taxable;     // line subtotal after prorated discount
        private BigDecimal rate;        // percentage; ZERO when no rule matched the line
        private UUID ruleId;            // null when no rule matched
        private String ruleName;
        private BigDecimal tax;
    }
}
