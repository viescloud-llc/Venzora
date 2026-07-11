package com.viescloud.llc.venzora.model.report;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefundsReport {

    private ReportPeriod period;
    private List<CurrencyBlock> byCurrency;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CurrencyBlock {
        private String currency;
        private int totalOrders;       // total orders in the period (denominator for refundRate)
        private int refundCount;       // orders that were fully or partially refunded
        private BigDecimal totalRefunded;
        private BigDecimal refundRate; // refundCount / totalOrders, two decimals
    }
}
