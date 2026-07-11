package com.viescloud.llc.venzora.model.report;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-jurisdiction tax summary. Drives the "file my sales tax" report. Numbers come
 * from what was actually charged on each {@code OrderFulfillment.tax}; the
 * {@code matchingRule} is resolved at report time against the current
 * {@code TaxRule} registry and is informational only.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaxReport {

    private ReportPeriod period;
    private List<CurrencyBlock> byCurrency;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CurrencyBlock {
        private String currency;
        private List<JurisdictionLine> jurisdictions;
        private Totals totals;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class JurisdictionLine {
        private String country;
        private String state;
        private String city;
        private String postalCode;
        private int orderCount;
        private BigDecimal grossSales;
        private BigDecimal taxableAmount;
        private BigDecimal taxCollected;
        private BigDecimal taxRefunded;
        private BigDecimal netTaxCollected;
        private BigDecimal effectiveRate;
        private MatchingRule matchingRule;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MatchingRule {
        private UUID id;
        private String name;
        private BigDecimal rate;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Totals {
        private int orderCount;
        private BigDecimal grossSales;
        private BigDecimal taxCollected;
        private BigDecimal taxRefunded;
        private BigDecimal netTaxCollected;
    }
}
