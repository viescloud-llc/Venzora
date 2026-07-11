package com.viescloud.llc.venzora.model.report;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Top-line KPIs over a date range, split per currency. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SalesSummaryReport {

    private ReportPeriod period;
    private List<CurrencyBlock> byCurrency;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CurrencyBlock {
        private String currency;
        private int orderCount;
        private BigDecimal grossRevenue;       // sum of subtotal
        private BigDecimal discounts;          // sum of discountAmount
        private BigDecimal tax;                // sum of tax
        private BigDecimal shipping;           // sum of shippingCost
        private BigDecimal totalGross;         // sum of totalAmount
        private BigDecimal averageOrderValue;  // totalGross / orderCount
        private int refundCount;
        private BigDecimal refundAmount;
    }
}
