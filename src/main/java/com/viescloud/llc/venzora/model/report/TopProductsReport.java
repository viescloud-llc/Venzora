package com.viescloud.llc.venzora.model.report;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopProductsReport {

    private ReportPeriod period;
    private String orderedBy;       // "revenue" | "quantity"
    private List<CurrencyBlock> byCurrency;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CurrencyBlock {
        private String currency;
        private List<Line> products;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Line {
        private UUID productId;
        private String name;
        private int unitsSold;
        private BigDecimal revenue;
    }
}
