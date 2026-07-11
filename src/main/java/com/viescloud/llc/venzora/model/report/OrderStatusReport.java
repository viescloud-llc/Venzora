package com.viescloud.llc.venzora.model.report;

import java.util.Map;

import com.viescloud.llc.venzora.model.product.type.FulfillmentStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderStatusReport {

    private ReportPeriod period;
    private int totalOrders;
    private Map<FulfillmentStatus, Integer> counts;
}
