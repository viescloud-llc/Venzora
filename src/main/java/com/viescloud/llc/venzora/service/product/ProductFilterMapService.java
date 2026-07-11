package com.viescloud.llc.venzora.service.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.viescloud.llc.venzora.dao.product.AttributeDefinitionDao;
import com.viescloud.llc.venzora.dao.product.CategoryDao;
import com.viescloud.llc.venzora.dao.product.ProductDao;
import com.viescloud.llc.venzora.dao.product.TagDao;
import com.viescloud.llc.venzora.model.product.AttributeDefinition;
import com.viescloud.llc.venzora.model.product.AttributeOption;
import com.viescloud.llc.venzora.model.product.Category;
import com.viescloud.llc.venzora.model.product.FilterKind;
import com.viescloud.llc.venzora.model.product.FilterMap;
import com.viescloud.llc.venzora.model.product.FilterOption;
import com.viescloud.llc.venzora.model.product.FilterRange;
import com.viescloud.llc.venzora.model.product.FilterSpec;
import com.viescloud.llc.venzora.model.product.Product;
import com.viescloud.llc.venzora.model.product.Tag;
import com.viescloud.llc.venzora.model.product.type.ProductAttributeType;
import com.viescloud.llc.venzora.model.product.type.ProductStatus;
import com.viescloud.llc.venzora.model.share_enum.Currency;

/**
 * Cached "everything you could filter on" payload for the storefront. Heavy to
 * compute (scans products + categories + tags + attribute definitions), so it's
 * kept in a volatile field and refreshed by a background scheduler at
 * {@value #REFRESH_INTERVAL_MS}ms. If a refresh throws, the previous cache is
 * kept rather than blanked — better to serve stale data than blow up.
 */
@Service
public class ProductFilterMapService {

    static final long REFRESH_INTERVAL_MS = 60_000L;

    private static final Logger log = LoggerFactory.getLogger(ProductFilterMapService.class);

    private final ProductDao productDao;
    private final CategoryDao categoryDao;
    private final TagDao tagDao;
    private final AttributeDefinitionDao attributeDefinitionDao;

    /** Latest computed snapshot. {@code volatile} for cross-thread visibility. */
    private volatile FilterMap cached;

    public ProductFilterMapService(ProductDao productDao,
                                   CategoryDao categoryDao,
                                   TagDao tagDao,
                                   AttributeDefinitionDao attributeDefinitionDao) {
        this.productDao = productDao;
        this.categoryDao = categoryDao;
        this.tagDao = tagDao;
        this.attributeDefinitionDao = attributeDefinitionDao;
    }

    /**
     * Serves the cached copy; triggers a synchronous compute if the cache is
     * cold (typically only on the very first request after startup, before the
     * scheduler has run).
     */
    public FilterMap get() {
        FilterMap snapshot = cached;
        if (snapshot == null) {
            refresh();
            snapshot = cached;
        }
        return snapshot;
    }

    /** Scheduled background refresh. Errors are swallowed (logged); stale cache stays. */
    @Scheduled(fixedRate = REFRESH_INTERVAL_MS)
    public void refresh() {
        try {
            cached = compute();
            log.debug("Refreshed product filter map ({} filters)", cached.getFilters().size());
        } catch (Exception e) {
            log.warn("Failed to refresh product filter map; keeping previous snapshot", e);
        }
    }

    @Transactional(readOnly = true)
    protected FilterMap compute() {
        List<FilterSpec> filters = new ArrayList<>();

        filters.add(textSearchSpec());
        filters.add(categorySpec());
        filters.add(tagsSpec());

        List<Product> activeProducts = productDao.findAll().stream()
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .toList();

        Set<Currency> currencies = new HashSet<>();
        for (Product p : activeProducts) {
            if (p.getCurrency() != null) currencies.add(p.getCurrency());
        }
        filters.add(currencySpec(currencies));
        filters.add(priceSpec(activeProducts, currencies));

        attributeDefinitionDao.findAll().stream()
                .sorted(Comparator.comparing(AttributeDefinition::getName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(this::attributeSpec)
                .forEach(filters::add);

        return new FilterMap(Instant.now(), filters);
    }

    // ---------- per-filter builders ----------

    private FilterSpec textSearchSpec() {
        return new FilterSpec(
                "q", null, "Search",
                FilterKind.TEXT_SEARCH,
                false, null, null,
                Map.of("appliesTo", "name+description"));
    }

    private FilterSpec categorySpec() {
        List<FilterOption> options = categoryDao.findAll().stream()
                .filter(c -> c.getId() != null && c.getName() != null)
                .sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
                .map(c -> new FilterOption(c.getId().toString(), c.getName()))
                .toList();
        return new FilterSpec(
                "categoryId", null, "Category",
                FilterKind.SINGLE_SELECT,
                false, options, null, Map.of());
    }

    private FilterSpec tagsSpec() {
        List<FilterOption> options = tagDao.findAll().stream()
                .filter(t -> t.getId() != null && t.getName() != null)
                .sorted(Comparator.comparing(Tag::getName, String.CASE_INSENSITIVE_ORDER))
                .map(t -> new FilterOption(t.getId().toString(), t.getName()))
                .toList();
        return new FilterSpec(
                "tagIds", null, "Tags",
                FilterKind.MULTI_SELECT,
                true, options, null, Map.of());
    }

    private FilterSpec currencySpec(Set<Currency> currencies) {
        List<FilterOption> options = currencies.stream()
                .sorted(Comparator.comparing(Currency::name))
                .map(c -> new FilterOption(c.name(), c.name()))
                .toList();
        return new FilterSpec(
                "currency", null, "Currency",
                FilterKind.SINGLE_SELECT,
                false, options, null, Map.of());
    }

    private FilterSpec priceSpec(List<Product> activeProducts, Set<Currency> currencies) {
        Map<String, FilterRange> ranges = new HashMap<>();
        for (Currency c : currencies) {
            BigDecimal min = null, max = null;
            for (Product p : activeProducts) {
                if (p.getCurrency() != c || p.getBasePrice() == null) continue;
                BigDecimal price = p.getBasePrice();
                if (min == null || price.compareTo(min) < 0) min = price;
                if (max == null || price.compareTo(max) > 0) max = price;
            }
            if (min != null) ranges.put(c.name(), new FilterRange(min, max));
        }
        return new FilterSpec(
                "minPrice", "maxPrice", "Price",
                FilterKind.RANGE_PRICE,
                false, null, ranges, Map.of());
    }

    private FilterSpec attributeSpec(AttributeDefinition def) {
        ProductAttributeType type = def.getType() == null
                ? ProductAttributeType.TEXT : def.getType();
        FilterKind kind = mapAttributeKind(type);
        List<FilterOption> options = null;
        if ((type == ProductAttributeType.SELECT || type == ProductAttributeType.MULTI_SELECT)
                && def.getOptions() != null) {
            options = def.getOptions().stream()
                    .filter(o -> o.getValue() != null)
                    .sorted(Comparator.comparing(AttributeOption::getSortOrder,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(o -> new FilterOption(
                            o.getValue(),
                            o.getDisplayValue() != null ? o.getDisplayValue() : o.getValue()))
                    .toList();
        }

        Map<String, String> meta = new HashMap<>();
        meta.put("attribute.type", type.name());
        if (def.getUnit() != null && !def.getUnit().isBlank()) {
            meta.put("attribute.unit", def.getUnit());
        }

        // Both SELECT and MULTI_SELECT are queryable with repeated values on the
        // server (OR-within-key). Frontends are free to render SELECT as a
        // dropdown anyway.
        boolean multi = type == ProductAttributeType.SELECT
                     || type == ProductAttributeType.MULTI_SELECT;

        String name = def.getName() == null ? "" : def.getName();
        String display = def.getDisplayName() != null ? def.getDisplayName() : name;
        return new FilterSpec(
                "attribute." + name,
                null,
                display,
                kind,
                multi,
                options,
                null,
                meta);
    }

    private static FilterKind mapAttributeKind(ProductAttributeType t) {
        return switch (t) {
            case TEXT        -> FilterKind.TEXT;
            case NUMBER      -> FilterKind.NUMBER;
            case BOOLEAN     -> FilterKind.BOOLEAN;
            case SELECT      -> FilterKind.SINGLE_SELECT;
            case MULTI_SELECT -> FilterKind.MULTI_SELECT;
            case DATE        -> FilterKind.DATE;
            case TIME        -> FilterKind.TIME;
            case DATE_TIME   -> FilterKind.DATE_TIME;
        };
    }
}
