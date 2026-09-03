package com.viescloud.llc.venzora.controller.report;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.interfaces.annotation.RequiresAuthority;

import com.viescloud.llc.venzora.model.report.CustomersReport;
import com.viescloud.llc.venzora.model.report.GeographyReport;
import com.viescloud.llc.venzora.model.report.OrderStatusReport;
import com.viescloud.llc.venzora.model.report.OrdersExportResponse;
import com.viescloud.llc.venzora.model.report.RefundsReport;
import com.viescloud.llc.venzora.model.report.SalesSummaryReport;
import com.viescloud.llc.venzora.model.report.SalesTimeseriesReport;
import com.viescloud.llc.venzora.model.report.TaxReport;
import com.viescloud.llc.venzora.model.report.TopCategoriesReport;
import com.viescloud.llc.venzora.model.report.TopProductsReport;
import com.viescloud.llc.venzora.service.report.ReportService;

/**
 * Read-only analytics endpoints. Every report takes the same {@code from} / {@code to}
 * ISO timestamps and returns raw JSON for the frontend to render.
 *
 * <p><strong>Auth note:</strong> these endpoints inherit no automatic admin gate from
 * the framework. In production, gate the entire {@code /reports} path at the reverse
 * proxy or place explicit permission checks here.
 */
@RequiresAuthority("reports:read")
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reports;

    public ReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/tax")
    public TaxReport tax(@RequestParam String from, @RequestParam String to) {
        return reports.tax(from, to);
    }

    @GetMapping("/sales/summary")
    public SalesSummaryReport salesSummary(@RequestParam String from, @RequestParam String to) {
        return reports.salesSummary(from, to);
    }

    @GetMapping("/sales/timeseries")
    public SalesTimeseriesReport salesTimeseries(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "day") String bucket) {
        return reports.salesTimeseries(from, to, bucket);
    }

    @GetMapping("/products/top")
    public TopProductsReport topProducts(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "revenue") String by,
            @RequestParam(defaultValue = "10") int limit) {
        return reports.topProducts(from, to, by, limit);
    }

    @GetMapping("/categories/top")
    public TopCategoriesReport topCategories(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "revenue") String by,
            @RequestParam(defaultValue = "10") int limit) {
        return reports.topCategories(from, to, by, limit);
    }

    @GetMapping("/geography")
    public GeographyReport geography(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "country") String groupBy) {
        return reports.geography(from, to, groupBy);
    }

    @GetMapping("/orders/status")
    public OrderStatusReport ordersStatus(@RequestParam String from, @RequestParam String to) {
        return reports.ordersStatus(from, to);
    }

    @GetMapping("/refunds")
    public RefundsReport refunds(@RequestParam String from, @RequestParam String to) {
        return reports.refunds(from, to);
    }

    @GetMapping("/customers/summary")
    public CustomersReport customers(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "10") int limit) {
        return reports.customers(from, to, limit);
    }

    @GetMapping("/orders")
    public OrdersExportResponse ordersExport(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return reports.ordersExport(from, to, page, size);
    }
}
