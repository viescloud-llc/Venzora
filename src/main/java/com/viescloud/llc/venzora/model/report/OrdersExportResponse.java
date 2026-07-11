package com.viescloud.llc.venzora.model.report;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.viescloud.eco.viesspringutils.util.DateTime;
import com.viescloud.llc.venzora.model.product.type.FulfillmentStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Denormalized order rows for power users to plug into Excel / Metabase / Looker. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrdersExportResponse {

    private ReportPeriod period;
    private int page;
    private int size;
    private int totalElements;
    private List<Row> rows;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Row {
        private UUID orderFulfillmentId;
        private UUID checkoutOrderId;
        private String orderNumber;
        private DateTime createdAt;
        private UUID userId;
        private String currency;
        private BigDecimal subtotal;
        private BigDecimal discountAmount;
        private BigDecimal tax;
        private BigDecimal shippingCost;
        private BigDecimal totalAmount;
        private FulfillmentStatus status;
        private String shippingCountry;
        private String shippingState;
        private String shippingCity;
        private String shippingPostalCode;
        private int itemCount;
    }
}
