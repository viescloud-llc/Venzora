package com.viescloud.llc.venzora.model.report;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Bucketed sales series for trend charts. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SalesTimeseriesReport {

    private ReportPeriod period;
    private String bucket;          // "day" | "week" | "month"
    private List<CurrencyBlock> byCurrency;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CurrencyBlock {
        private String currency;
        private List<Point> points;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Point {
        private String bucket;       // e.g. "2026-01-15" (day or week-Monday) or "2026-01" (month)
        private int orderCount;
        private BigDecimal revenue;  // sum of totalAmount
        private BigDecimal tax;
    }
}
