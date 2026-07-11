package com.viescloud.llc.venzora.model.report;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeographyReport {

    private ReportPeriod period;
    private String groupBy;          // "country" | "state" | "city"
    private List<CurrencyBlock> byCurrency;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CurrencyBlock {
        private String currency;
        private List<Line> locations;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Line {
        private String country;
        private String state;       // null when groupBy=country
        private String city;        // null when groupBy in {country, state}
        private int orderCount;
        private BigDecimal revenue;
    }
}
