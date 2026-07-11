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
public class CustomersReport {

    private ReportPeriod period;
    private int totalCustomers;
    private List<CurrencyBlock> byCurrency;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CurrencyBlock {
        private String currency;
        private List<TopCustomer> topByRevenue;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TopCustomer {
        private UUID userId;
        private int orderCount;
        private BigDecimal revenue;
    }
}
