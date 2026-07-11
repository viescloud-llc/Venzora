package com.viescloud.llc.venzora.service.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.viescloud.eco.viesspringutils.auto.dao.checkout.CheckoutOrderDao;
import com.viescloud.eco.viesspringutils.auto.model.checkout.CheckoutOrder;
import com.viescloud.eco.viesspringutils.util.DateTime;
import com.viescloud.llc.venzora.dao.product.OrderFulfillmentDao;
import com.viescloud.llc.venzora.dao.product.TaxRuleDao;
import com.viescloud.llc.venzora.model.address.Address;
import com.viescloud.llc.venzora.model.product.Category;
import com.viescloud.llc.venzora.model.product.OrderFulfillment;
import com.viescloud.llc.venzora.model.product.OrderFulfillmentItem;
import com.viescloud.llc.venzora.model.product.Product;
import com.viescloud.llc.venzora.model.product.TaxRule;
import com.viescloud.llc.venzora.model.product.type.FulfillmentStatus;
import com.viescloud.llc.venzora.model.report.CustomersReport;
import com.viescloud.llc.venzora.model.report.GeographyReport;
import com.viescloud.llc.venzora.model.report.OrdersExportResponse;
import com.viescloud.llc.venzora.model.report.OrderStatusReport;
import com.viescloud.llc.venzora.model.report.RefundsReport;
import com.viescloud.llc.venzora.model.report.ReportPeriod;
import com.viescloud.llc.venzora.model.report.SalesSummaryReport;
import com.viescloud.llc.venzora.model.report.SalesTimeseriesReport;
import com.viescloud.llc.venzora.model.report.TaxReport;
import com.viescloud.llc.venzora.model.report.TopCategoriesReport;
import com.viescloud.llc.venzora.model.report.TopProductsReport;

/**
 * Read-only statistics across {@link OrderFulfillment}. Filtering is done in-memory:
 * load orders, prune by date range using {@link DateTime} comparison, then aggregate.
 * Fine for the hundreds-of-thousands-of-orders scale; for larger tenants, add a
 * timestamp index or pre-aggregated daily totals.
 */
@Service
public class ReportService {

    /** Statuses that mean "payment was actually captured" — the eligible set for revenue reports. */
    private static final Set<FulfillmentStatus> CAPTURED_STATUSES = Set.of(
            FulfillmentStatus.PROCESSING,
            FulfillmentStatus.SHIPPED,
            FulfillmentStatus.DELIVERED,
            FulfillmentStatus.RETURNED,
            FulfillmentStatus.REFUNDED,
            FulfillmentStatus.PARTIALLY_REFUNDED);

    private static final Set<FulfillmentStatus> REFUNDED_STATUSES = Set.of(
            FulfillmentStatus.REFUNDED,
            FulfillmentStatus.PARTIALLY_REFUNDED);

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final OrderFulfillmentDao fulfillmentDao;
    private final TaxRuleDao taxRuleDao;
    private final ObjectProvider<CheckoutOrderDao> checkoutOrderDaoProvider;

    public ReportService(OrderFulfillmentDao fulfillmentDao,
                         TaxRuleDao taxRuleDao,
                         ObjectProvider<CheckoutOrderDao> checkoutOrderDaoProvider) {
        this.fulfillmentDao = fulfillmentDao;
        this.taxRuleDao = taxRuleDao;
        this.checkoutOrderDaoProvider = checkoutOrderDaoProvider;
    }

    // ---------- Tax ----------

    @Transactional(readOnly = true)
    public TaxReport tax(String from, String to) {
        DateTime fromDt = parsePeriod(from, "from");
        DateTime toDt = parsePeriod(to, "to");
        List<OrderFulfillment> orders = loadInPeriod(fromDt, toDt, CAPTURED_STATUSES);
        Map<UUID, BigDecimal> refunds = refundAmountsFor(orders);

        // Group by currency, then by jurisdiction tuple.
        Map<String, Map<JurisdictionKey, JurisdictionAccumulator>> byCurrency = new HashMap<>();
        for (OrderFulfillment o : orders) {
            String currency = currencyOf(o);
            JurisdictionKey jk = JurisdictionKey.from(o.getShippingAddress());
            JurisdictionAccumulator acc = byCurrency
                    .computeIfAbsent(currency, k -> new HashMap<>())
                    .computeIfAbsent(jk, k -> new JurisdictionAccumulator(k));
            acc.add(o, refundForOrder(o, refunds));
        }

        List<TaxRule> activeRules = taxRuleDao.findAllByActiveTrue();

        List<TaxReport.CurrencyBlock> blocks = new ArrayList<>();
        for (var entry : byCurrency.entrySet()) {
            String currency = entry.getKey();
            List<TaxReport.JurisdictionLine> lines = entry.getValue().values().stream()
                    .map(acc -> acc.toLine(activeRules))
                    .sorted(Comparator
                            .comparing((TaxReport.JurisdictionLine l) -> l.getTaxCollected())
                            .reversed())
                    .toList();
            TaxReport.Totals totals = totalsOf(lines);
            blocks.add(new TaxReport.CurrencyBlock(currency, lines, totals));
        }
        blocks.sort(Comparator.comparing(TaxReport.CurrencyBlock::getCurrency));

        return new TaxReport(new ReportPeriod(from, to), blocks);
    }

    private TaxReport.Totals totalsOf(List<TaxReport.JurisdictionLine> lines) {
        int orderCount = 0;
        BigDecimal gross = BigDecimal.ZERO, taxCollected = BigDecimal.ZERO;
        BigDecimal taxRefunded = BigDecimal.ZERO, net = BigDecimal.ZERO;
        for (var l : lines) {
            orderCount += l.getOrderCount();
            gross = gross.add(l.getGrossSales());
            taxCollected = taxCollected.add(l.getTaxCollected());
            taxRefunded = taxRefunded.add(l.getTaxRefunded());
            net = net.add(l.getNetTaxCollected());
        }
        return new TaxReport.Totals(orderCount, gross, taxCollected, taxRefunded, net);
    }

    // ---------- Sales summary ----------

    @Transactional(readOnly = true)
    public SalesSummaryReport salesSummary(String from, String to) {
        DateTime fromDt = parsePeriod(from, "from");
        DateTime toDt = parsePeriod(to, "to");
        List<OrderFulfillment> orders = loadInPeriod(fromDt, toDt, CAPTURED_STATUSES);
        Map<UUID, BigDecimal> refunds = refundAmountsFor(orders);

        Map<String, SalesSummaryAccumulator> byCurrency = new HashMap<>();
        for (OrderFulfillment o : orders) {
            String currency = currencyOf(o);
            byCurrency.computeIfAbsent(currency, k -> new SalesSummaryAccumulator(currency))
                    .add(o, refundForOrder(o, refunds));
        }

        List<SalesSummaryReport.CurrencyBlock> blocks = byCurrency.values().stream()
                .map(SalesSummaryAccumulator::toBlock)
                .sorted(Comparator.comparing(SalesSummaryReport.CurrencyBlock::getCurrency))
                .toList();
        return new SalesSummaryReport(new ReportPeriod(from, to), blocks);
    }

    // ---------- Sales timeseries ----------

    @Transactional(readOnly = true)
    public SalesTimeseriesReport salesTimeseries(String from, String to, String bucket) {
        String normalized = bucket == null ? "day" : bucket.toLowerCase(Locale.ROOT);
        if (!Set.of("day", "week", "month").contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "bucket must be one of: day, week, month");
        }
        DateTime fromDt = parsePeriod(from, "from");
        DateTime toDt = parsePeriod(to, "to");
        List<OrderFulfillment> orders = loadInPeriod(fromDt, toDt, CAPTURED_STATUSES);

        // currency → bucket key → accumulator
        Map<String, Map<String, TimeseriesAcc>> bucketed = new HashMap<>();
        for (OrderFulfillment o : orders) {
            String currency = currencyOf(o);
            String key = bucketKey(o.getCreatedAt(), normalized);
            if (key == null) continue;
            bucketed.computeIfAbsent(currency, k -> new HashMap<>())
                    .computeIfAbsent(key, k -> new TimeseriesAcc(key))
                    .add(o);
        }

        List<SalesTimeseriesReport.CurrencyBlock> blocks = new ArrayList<>();
        for (var entry : bucketed.entrySet()) {
            List<SalesTimeseriesReport.Point> points = entry.getValue().values().stream()
                    .sorted(Comparator.comparing(a -> a.bucket))
                    .map(TimeseriesAcc::toPoint)
                    .toList();
            blocks.add(new SalesTimeseriesReport.CurrencyBlock(entry.getKey(), points));
        }
        blocks.sort(Comparator.comparing(SalesTimeseriesReport.CurrencyBlock::getCurrency));

        return new SalesTimeseriesReport(new ReportPeriod(from, to), normalized, blocks);
    }

    // ---------- Top products ----------

    @Transactional(readOnly = true)
    public TopProductsReport topProducts(String from, String to, String by, int limit) {
        String orderedBy = normalizeOrderBy(by);
        int safeLimit = limit <= 0 ? 10 : Math.min(limit, 200);
        DateTime fromDt = parsePeriod(from, "from");
        DateTime toDt = parsePeriod(to, "to");
        List<OrderFulfillment> orders = loadInPeriod(fromDt, toDt, CAPTURED_STATUSES);

        Map<String, Map<UUID, ProductAcc>> byCurrency = new HashMap<>();
        for (OrderFulfillment o : orders) {
            String currency = currencyOf(o);
            for (OrderFulfillmentItem item : o.getItems()) {
                Product p = item.getProductVariant() == null ? null : item.getProductVariant().getProduct();
                if (p == null) continue;
                byCurrency.computeIfAbsent(currency, k -> new HashMap<>())
                        .computeIfAbsent(p.getId(), id -> new ProductAcc(id, p.getName()))
                        .add(item);
            }
        }

        List<TopProductsReport.CurrencyBlock> blocks = new ArrayList<>();
        for (var entry : byCurrency.entrySet()) {
            List<TopProductsReport.Line> lines = entry.getValue().values().stream()
                    .sorted(comparatorFor(orderedBy))
                    .limit(safeLimit)
                    .map(ProductAcc::toLine)
                    .toList();
            blocks.add(new TopProductsReport.CurrencyBlock(entry.getKey(), lines));
        }
        blocks.sort(Comparator.comparing(TopProductsReport.CurrencyBlock::getCurrency));

        return new TopProductsReport(new ReportPeriod(from, to), orderedBy, blocks);
    }

    private Comparator<ProductAcc> comparatorFor(String orderedBy) {
        return "quantity".equals(orderedBy)
                ? Comparator.<ProductAcc>comparingInt(a -> a.units).reversed()
                : Comparator.<ProductAcc, BigDecimal>comparing(a -> a.revenue).reversed();
    }

    // ---------- Top categories ----------

    @Transactional(readOnly = true)
    public TopCategoriesReport topCategories(String from, String to, String by, int limit) {
        String orderedBy = normalizeOrderBy(by);
        int safeLimit = limit <= 0 ? 10 : Math.min(limit, 200);
        DateTime fromDt = parsePeriod(from, "from");
        DateTime toDt = parsePeriod(to, "to");
        List<OrderFulfillment> orders = loadInPeriod(fromDt, toDt, CAPTURED_STATUSES);

        Map<String, Map<UUID, CategoryAcc>> byCurrency = new HashMap<>();
        for (OrderFulfillment o : orders) {
            String currency = currencyOf(o);
            for (OrderFulfillmentItem item : o.getItems()) {
                Product p = item.getProductVariant() == null ? null : item.getProductVariant().getProduct();
                Category c = p == null ? null : p.getCategory();
                if (c == null) continue;
                byCurrency.computeIfAbsent(currency, k -> new HashMap<>())
                        .computeIfAbsent(c.getId(), id -> new CategoryAcc(id, c.getName()))
                        .add(item);
            }
        }

        List<TopCategoriesReport.CurrencyBlock> blocks = new ArrayList<>();
        for (var entry : byCurrency.entrySet()) {
            Comparator<CategoryAcc> cmp = "quantity".equals(orderedBy)
                    ? Comparator.<CategoryAcc>comparingInt(a -> a.units).reversed()
                    : Comparator.<CategoryAcc, BigDecimal>comparing(a -> a.revenue).reversed();
            List<TopCategoriesReport.Line> lines = entry.getValue().values().stream()
                    .sorted(cmp)
                    .limit(safeLimit)
                    .map(CategoryAcc::toLine)
                    .toList();
            blocks.add(new TopCategoriesReport.CurrencyBlock(entry.getKey(), lines));
        }
        blocks.sort(Comparator.comparing(TopCategoriesReport.CurrencyBlock::getCurrency));

        return new TopCategoriesReport(new ReportPeriod(from, to), orderedBy, blocks);
    }

    // ---------- Geography ----------

    @Transactional(readOnly = true)
    public GeographyReport geography(String from, String to, String groupBy) {
        String level = groupBy == null ? "country" : groupBy.toLowerCase(Locale.ROOT);
        if (!Set.of("country", "state", "city").contains(level)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "groupBy must be one of: country, state, city");
        }
        DateTime fromDt = parsePeriod(from, "from");
        DateTime toDt = parsePeriod(to, "to");
        List<OrderFulfillment> orders = loadInPeriod(fromDt, toDt, CAPTURED_STATUSES);

        Map<String, Map<GeoKey, GeoAcc>> byCurrency = new HashMap<>();
        for (OrderFulfillment o : orders) {
            String currency = currencyOf(o);
            GeoKey gk = GeoKey.from(o.getShippingAddress(), level);
            byCurrency.computeIfAbsent(currency, k -> new HashMap<>())
                    .computeIfAbsent(gk, k -> new GeoAcc(k))
                    .add(o);
        }

        List<GeographyReport.CurrencyBlock> blocks = new ArrayList<>();
        for (var entry : byCurrency.entrySet()) {
            List<GeographyReport.Line> lines = entry.getValue().values().stream()
                    .sorted(Comparator.<GeoAcc, BigDecimal>comparing(a -> a.revenue).reversed())
                    .map(GeoAcc::toLine)
                    .toList();
            blocks.add(new GeographyReport.CurrencyBlock(entry.getKey(), lines));
        }
        blocks.sort(Comparator.comparing(GeographyReport.CurrencyBlock::getCurrency));

        return new GeographyReport(new ReportPeriod(from, to), level, blocks);
    }

    // ---------- Order status ----------

    @Transactional(readOnly = true)
    public OrderStatusReport ordersStatus(String from, String to) {
        DateTime fromDt = parsePeriod(from, "from");
        DateTime toDt = parsePeriod(to, "to");
        List<OrderFulfillment> orders = loadInPeriod(fromDt, toDt, null);  // include all statuses

        Map<FulfillmentStatus, Integer> counts = new EnumMap<>(FulfillmentStatus.class);
        for (var s : FulfillmentStatus.values()) counts.put(s, 0);
        for (OrderFulfillment o : orders) {
            FulfillmentStatus s = o.getStatus();
            if (s == null) continue;
            counts.merge(s, 1, Integer::sum);
        }
        return new OrderStatusReport(new ReportPeriod(from, to), orders.size(), counts);
    }

    // ---------- Refunds ----------

    @Transactional(readOnly = true)
    public RefundsReport refunds(String from, String to) {
        DateTime fromDt = parsePeriod(from, "from");
        DateTime toDt = parsePeriod(to, "to");
        List<OrderFulfillment> orders = loadInPeriod(fromDt, toDt, CAPTURED_STATUSES);
        Map<UUID, BigDecimal> refundAmounts = refundAmountsFor(orders);

        Map<String, RefundAcc> byCurrency = new HashMap<>();
        for (OrderFulfillment o : orders) {
            String currency = currencyOf(o);
            RefundAcc acc = byCurrency.computeIfAbsent(currency, k -> new RefundAcc(currency));
            acc.totalOrders++;
            BigDecimal r = refundForOrder(o, refundAmounts);
            if (r.signum() > 0 || REFUNDED_STATUSES.contains(o.getStatus())) {
                acc.refundCount++;
                acc.totalRefunded = acc.totalRefunded.add(r);
            }
        }

        List<RefundsReport.CurrencyBlock> blocks = byCurrency.values().stream()
                .map(RefundAcc::toBlock)
                .sorted(Comparator.comparing(RefundsReport.CurrencyBlock::getCurrency))
                .toList();
        return new RefundsReport(new ReportPeriod(from, to), blocks);
    }

    // ---------- Customers ----------

    @Transactional(readOnly = true)
    public CustomersReport customers(String from, String to, int topLimit) {
        int safeLimit = topLimit <= 0 ? 10 : Math.min(topLimit, 200);
        DateTime fromDt = parsePeriod(from, "from");
        DateTime toDt = parsePeriod(to, "to");
        List<OrderFulfillment> orders = loadInPeriod(fromDt, toDt, CAPTURED_STATUSES);

        Set<UUID> uniqueCustomers = new HashSet<>();
        Map<String, Map<UUID, CustomerAcc>> byCurrency = new HashMap<>();
        for (OrderFulfillment o : orders) {
            if (o.getUserId() != null) uniqueCustomers.add(o.getUserId());
            String currency = currencyOf(o);
            byCurrency.computeIfAbsent(currency, k -> new HashMap<>())
                    .computeIfAbsent(o.getUserId(), id -> new CustomerAcc(id))
                    .add(o);
        }

        List<CustomersReport.CurrencyBlock> blocks = new ArrayList<>();
        for (var entry : byCurrency.entrySet()) {
            List<CustomersReport.TopCustomer> top = entry.getValue().values().stream()
                    .sorted(Comparator.<CustomerAcc, BigDecimal>comparing(a -> a.revenue).reversed())
                    .limit(safeLimit)
                    .map(CustomerAcc::toTop)
                    .toList();
            blocks.add(new CustomersReport.CurrencyBlock(entry.getKey(), top));
        }
        blocks.sort(Comparator.comparing(CustomersReport.CurrencyBlock::getCurrency));

        return new CustomersReport(new ReportPeriod(from, to), uniqueCustomers.size(), blocks);
    }

    // ---------- Raw export ----------

    @Transactional(readOnly = true)
    public OrdersExportResponse ordersExport(String from, String to, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 100 : Math.min(size, 1000);
        DateTime fromDt = parsePeriod(from, "from");
        DateTime toDt = parsePeriod(to, "to");
        List<OrderFulfillment> orders = loadInPeriod(fromDt, toDt, null);

        int total = orders.size();
        int fromIdx = Math.min(safePage * safeSize, total);
        int toIdx = Math.min(fromIdx + safeSize, total);
        List<OrdersExportResponse.Row> rows = orders.subList(fromIdx, toIdx).stream()
                .map(ReportService::toExportRow)
                .toList();
        return new OrdersExportResponse(new ReportPeriod(from, to),
                safePage, safeSize, total, rows);
    }

    // ===== loading & date helpers =====

    private List<OrderFulfillment> loadInPeriod(DateTime fromDt,
                                                DateTime toDt,
                                                Set<FulfillmentStatus> statusFilter) {
        return fulfillmentDao.findAll().stream()
                .filter(o -> o.getCreatedAt() != null)
                .filter(o -> !o.getCreatedAt().isBefore(fromDt))
                .filter(o -> !o.getCreatedAt().isAfter(toDt))
                .filter(o -> statusFilter == null || statusFilter.contains(o.getStatus()))
                .toList();
    }

    private static DateTime parsePeriod(String iso, String label) {
        if (iso == null || iso.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    label + " is required (ISO 8601: 2026-01-01 or 2026-01-01T00:00:00Z)");
        }
        // Date-only
        if (iso.length() == 10) {
            try {
                LocalDate d = LocalDate.parse(iso);
                return DateTime.of(d.atStartOfDay());
            } catch (Exception ignored) { }
        }
        // Instant with Z
        try {
            Instant inst = Instant.parse(iso);
            return DateTime.of(LocalDateTime.ofInstant(inst, ZoneOffset.UTC));
        } catch (Exception ignored) { }
        // Plain LocalDateTime
        try {
            return DateTime.of(LocalDateTime.parse(iso));
        } catch (Exception ignored) { }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid " + label + " format: " + iso);
    }

    private static String bucketKey(DateTime dt, String bucket) {
        if (dt == null || dt.getYear() == null || dt.getMonth() == null || dt.getDay() == null) {
            return null;
        }
        try {
            LocalDate d = LocalDate.of(dt.getYear(), dt.getMonth(), dt.getDay());
            switch (bucket) {
                case "day":
                    return d.format(DateTimeFormatter.ISO_LOCAL_DATE);
                case "week":
                    LocalDate monday = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    return monday.format(DateTimeFormatter.ISO_LOCAL_DATE);
                case "month":
                    return String.format("%04d-%02d", dt.getYear(), dt.getMonth());
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static String currencyOf(OrderFulfillment o) {
        return o.getCurrency() == null ? "UNKNOWN" : o.getCurrency().name();
    }

    private static String normalizeOrderBy(String by) {
        if (by == null) return "revenue";
        String s = by.toLowerCase(Locale.ROOT);
        if (!Set.of("revenue", "quantity").contains(s)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "by must be one of: revenue, quantity");
        }
        return s;
    }

    // ===== refund accounting =====

    /**
     * Returns {@code orderFulfillmentId → amountRefunded} by joining each order's
     * {@code checkoutOrderId} against the library's {@link CheckoutOrder} table. If
     * the checkout module is not registered, returns an empty map (and refund
     * subtraction is silently skipped, with the report still being internally
     * consistent — it just shows no refunds).
     */
    private Map<UUID, BigDecimal> refundAmountsFor(List<OrderFulfillment> orders) {
        CheckoutOrderDao dao = checkoutOrderDaoProvider.getIfAvailable();
        if (dao == null) return Map.of();

        Set<UUID> checkoutIds = orders.stream()
                .map(OrderFulfillment::getCheckoutOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (checkoutIds.isEmpty()) return Map.of();

        Map<UUID, BigDecimal> byCheckoutId = new HashMap<>();
        for (CheckoutOrder co : dao.findAllById(checkoutIds)) {
            BigDecimal refunded = co.getAmountRefunded();
            if (refunded != null && refunded.signum() > 0) {
                byCheckoutId.put(co.getId(), refunded);
            }
        }

        Map<UUID, BigDecimal> byFulfillmentId = new HashMap<>();
        for (OrderFulfillment o : orders) {
            BigDecimal refunded = o.getCheckoutOrderId() == null
                    ? null : byCheckoutId.get(o.getCheckoutOrderId());
            if (refunded != null) {
                byFulfillmentId.put(o.getId(), refunded);
            }
        }
        return byFulfillmentId;
    }

    private static BigDecimal refundForOrder(OrderFulfillment o, Map<UUID, BigDecimal> refunds) {
        BigDecimal v = refunds.get(o.getId());
        return v == null ? BigDecimal.ZERO : v;
    }

    private static OrdersExportResponse.Row toExportRow(OrderFulfillment o) {
        Address sa = o.getShippingAddress();
        OrdersExportResponse.Row r = new OrdersExportResponse.Row();
        r.setOrderFulfillmentId(o.getId());
        r.setCheckoutOrderId(o.getCheckoutOrderId());
        r.setOrderNumber(o.getOrderNumber());
        r.setCreatedAt(o.getCreatedAt());
        r.setUserId(o.getUserId());
        r.setCurrency(currencyOf(o));
        r.setSubtotal(o.getSubtotal());
        r.setDiscountAmount(o.getDiscountAmount());
        r.setTax(o.getTax());
        r.setShippingCost(o.getShippingCost());
        r.setTotalAmount(o.getTotalAmount());
        r.setStatus(o.getStatus());
        if (sa != null) {
            r.setShippingCountry(sa.getCountry());
            r.setShippingState(sa.getState());
            r.setShippingCity(sa.getCity());
            r.setShippingPostalCode(sa.getPostalCode());
        }
        r.setItemCount(o.getItems() == null ? 0 : o.getItems().size());
        return r;
    }

    // ===== inner accumulators =====

    /** Jurisdiction tuple used to group tax data by shipping address. */
    private static final class JurisdictionKey {
        final String country, state, city, postalCode;
        JurisdictionKey(String country, String state, String city, String postalCode) {
            this.country = country;
            this.state = state;
            this.city = city;
            this.postalCode = postalCode;
        }
        static JurisdictionKey from(Address a) {
            if (a == null) return new JurisdictionKey(null, null, null, null);
            return new JurisdictionKey(
                    blank(a.getCountry()), blank(a.getState()),
                    blank(a.getCity()), blank(a.getPostalCode()));
        }
        static String blank(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof JurisdictionKey other)) return false;
            return Objects.equals(country, other.country)
                    && Objects.equals(state, other.state)
                    && Objects.equals(city, other.city)
                    && Objects.equals(postalCode, other.postalCode);
        }
        @Override public int hashCode() { return Objects.hash(country, state, city, postalCode); }
    }

    private static final class JurisdictionAccumulator {
        final JurisdictionKey key;
        int orderCount;
        BigDecimal grossSales = BigDecimal.ZERO;
        BigDecimal taxableAmount = BigDecimal.ZERO;
        BigDecimal taxCollected = BigDecimal.ZERO;
        BigDecimal taxRefunded = BigDecimal.ZERO;

        JurisdictionAccumulator(JurisdictionKey k) { this.key = k; }

        void add(OrderFulfillment o, BigDecimal refundedAmount) {
            orderCount++;
            BigDecimal subtotal = nz(o.getSubtotal());
            BigDecimal discount = nz(o.getDiscountAmount());
            BigDecimal tax = nz(o.getTax());
            BigDecimal total = nz(o.getTotalAmount());
            grossSales = grossSales.add(subtotal);
            taxableAmount = taxableAmount.add(subtotal.subtract(discount));
            taxCollected = taxCollected.add(tax);
            if (refundedAmount.signum() > 0 && total.signum() > 0) {
                BigDecimal share = refundedAmount.divide(total, 6, RoundingMode.HALF_UP);
                taxRefunded = taxRefunded.add(tax.multiply(share).setScale(2, RoundingMode.HALF_UP));
            }
        }

        TaxReport.JurisdictionLine toLine(List<TaxRule> activeRules) {
            BigDecimal net = taxCollected.subtract(taxRefunded);
            BigDecimal effective = taxableAmount.signum() > 0
                    ? taxCollected.multiply(HUNDRED).divide(taxableAmount, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            TaxReport.MatchingRule matching = matchingRuleFor(key, activeRules);
            return new TaxReport.JurisdictionLine(
                    key.country, key.state, key.city, key.postalCode,
                    orderCount, grossSales, taxableAmount,
                    taxCollected, taxRefunded, net, effective, matching);
        }
    }

    /** Picks the most-specific currently-active TaxRule that matches a jurisdiction tuple. */
    private static TaxReport.MatchingRule matchingRuleFor(JurisdictionKey k, List<TaxRule> active) {
        if (active.isEmpty()) return null;
        TaxRule best = null;
        int bestScore = -1;
        for (TaxRule r : active) {
            if (!eq(r.getCountry(), k.country)
                    || !eq(r.getState(), k.state)
                    || !eq(r.getCity(), k.city)
                    || !eq(r.getPostalCode(), k.postalCode)) continue;
            int score = 0;
            if (r.getCountry() != null) score++;
            if (r.getState() != null) score++;
            if (r.getCity() != null) score++;
            if (r.getPostalCode() != null) score++;
            if (score > bestScore || (score == bestScore && best != null
                    && r.getPriority() != null && best.getPriority() != null
                    && r.getPriority() > best.getPriority())) {
                best = r;
                bestScore = score;
            }
        }
        return best == null ? null
                : new TaxReport.MatchingRule(best.getId(), best.getName(), best.getRate());
    }

    /** Matcher equality used by matchingRuleFor: null matcher matches anything. */
    private static boolean eq(String matcher, String actual) {
        if (matcher == null) return true;
        if (actual == null) return false;
        return matcher.trim().equalsIgnoreCase(actual.trim());
    }

    private static final class SalesSummaryAccumulator {
        final String currency;
        int orderCount, refundCount;
        BigDecimal grossRevenue = BigDecimal.ZERO, discounts = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO, shipping = BigDecimal.ZERO;
        BigDecimal totalGross = BigDecimal.ZERO, refundAmount = BigDecimal.ZERO;

        SalesSummaryAccumulator(String currency) { this.currency = currency; }

        void add(OrderFulfillment o, BigDecimal refunded) {
            orderCount++;
            grossRevenue = grossRevenue.add(nz(o.getSubtotal()));
            discounts = discounts.add(nz(o.getDiscountAmount()));
            tax = tax.add(nz(o.getTax()));
            shipping = shipping.add(nz(o.getShippingCost()));
            totalGross = totalGross.add(nz(o.getTotalAmount()));
            if (refunded.signum() > 0 || REFUNDED_STATUSES.contains(o.getStatus())) {
                refundCount++;
                refundAmount = refundAmount.add(refunded);
            }
        }

        SalesSummaryReport.CurrencyBlock toBlock() {
            BigDecimal avg = orderCount > 0
                    ? totalGross.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            return new SalesSummaryReport.CurrencyBlock(
                    currency, orderCount, grossRevenue, discounts, tax, shipping,
                    totalGross, avg, refundCount, refundAmount);
        }
    }

    private static final class TimeseriesAcc {
        final String bucket;
        int orderCount;
        BigDecimal revenue = BigDecimal.ZERO, tax = BigDecimal.ZERO;

        TimeseriesAcc(String bucket) { this.bucket = bucket; }

        void add(OrderFulfillment o) {
            orderCount++;
            revenue = revenue.add(nz(o.getTotalAmount()));
            tax = tax.add(nz(o.getTax()));
        }
        SalesTimeseriesReport.Point toPoint() {
            return new SalesTimeseriesReport.Point(bucket, orderCount, revenue, tax);
        }
    }

    private static final class ProductAcc {
        final UUID productId;
        final String name;
        int units;
        BigDecimal revenue = BigDecimal.ZERO;

        ProductAcc(UUID id, String name) { this.productId = id; this.name = name; }

        void add(OrderFulfillmentItem item) {
            units += item.getQuantity() == null ? 0 : item.getQuantity();
            revenue = revenue.add(nz(item.getTotalPrice()));
        }
        TopProductsReport.Line toLine() {
            return new TopProductsReport.Line(productId, name, units, revenue);
        }
    }

    private static final class CategoryAcc {
        final UUID categoryId;
        final String name;
        int units;
        BigDecimal revenue = BigDecimal.ZERO;

        CategoryAcc(UUID id, String name) { this.categoryId = id; this.name = name; }

        void add(OrderFulfillmentItem item) {
            units += item.getQuantity() == null ? 0 : item.getQuantity();
            revenue = revenue.add(nz(item.getTotalPrice()));
        }
        TopCategoriesReport.Line toLine() {
            return new TopCategoriesReport.Line(categoryId, name, units, revenue);
        }
    }

    private static final class GeoKey {
        final String country, state, city;
        GeoKey(String country, String state, String city) {
            this.country = country;
            this.state = state;
            this.city = city;
        }
        static GeoKey from(Address a, String level) {
            String country = a == null ? null : JurisdictionKey.blank(a.getCountry());
            String state = a == null ? null : JurisdictionKey.blank(a.getState());
            String city = a == null ? null : JurisdictionKey.blank(a.getCity());
            return switch (level) {
                case "country" -> new GeoKey(country, null, null);
                case "state"   -> new GeoKey(country, state, null);
                default        -> new GeoKey(country, state, city);
            };
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GeoKey g)) return false;
            return Objects.equals(country, g.country)
                    && Objects.equals(state, g.state)
                    && Objects.equals(city, g.city);
        }
        @Override public int hashCode() { return Objects.hash(country, state, city); }
    }

    private static final class GeoAcc {
        final GeoKey key;
        int orderCount;
        BigDecimal revenue = BigDecimal.ZERO;

        GeoAcc(GeoKey k) { this.key = k; }

        void add(OrderFulfillment o) {
            orderCount++;
            revenue = revenue.add(nz(o.getTotalAmount()));
        }
        GeographyReport.Line toLine() {
            return new GeographyReport.Line(key.country, key.state, key.city, orderCount, revenue);
        }
    }

    private static final class RefundAcc {
        final String currency;
        int totalOrders, refundCount;
        BigDecimal totalRefunded = BigDecimal.ZERO;

        RefundAcc(String c) { this.currency = c; }

        RefundsReport.CurrencyBlock toBlock() {
            BigDecimal rate = totalOrders > 0
                    ? BigDecimal.valueOf(refundCount)
                        .divide(BigDecimal.valueOf(totalOrders), 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            return new RefundsReport.CurrencyBlock(
                    currency, totalOrders, refundCount, totalRefunded, rate);
        }
    }

    private static final class CustomerAcc {
        final UUID userId;
        int orderCount;
        BigDecimal revenue = BigDecimal.ZERO;

        CustomerAcc(UUID id) { this.userId = id; }

        void add(OrderFulfillment o) {
            orderCount++;
            revenue = revenue.add(nz(o.getTotalAmount()));
        }
        CustomersReport.TopCustomer toTop() {
            return new CustomersReport.TopCustomer(userId, orderCount, revenue);
        }
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
