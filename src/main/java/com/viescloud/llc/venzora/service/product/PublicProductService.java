package com.viescloud.llc.venzora.service.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import com.viescloud.eco.viesspringutils.model.PageResponse;
import com.viescloud.llc.venzora.dao.product.AttributeDefinitionDao;
import com.viescloud.llc.venzora.dao.product.CategoryDao;
import com.viescloud.llc.venzora.dao.product.ProductDao;
import com.viescloud.llc.venzora.model.product.AttributeDefinition;
import com.viescloud.llc.venzora.model.product.AttributeOption;
import com.viescloud.llc.venzora.model.product.AttributeValue;
import com.viescloud.llc.venzora.model.product.Category;
import com.viescloud.llc.venzora.model.product.Product;
import com.viescloud.llc.venzora.model.product.ProductAttribute;
import com.viescloud.llc.venzora.model.product.ProductFiltersResponse;
import com.viescloud.llc.venzora.model.product.ProductListRequest;
import com.viescloud.llc.venzora.model.product.ProductVariant;
import com.viescloud.llc.venzora.model.product.ProductVariantAttribute;
import com.viescloud.llc.venzora.model.product.Tag;
import com.viescloud.llc.venzora.model.product.type.ProductStatus;
import com.viescloud.llc.venzora.model.share_enum.Currency;

/**
 * Read-only storefront query service. Returns only {@link ProductStatus#ACTIVE}
 * products and supports faceted filtering against the dynamic attribute schema:
 * pass {@code attribute.<DefinitionName>=<value>} repeated times to AND across
 * attributes and OR within a single attribute (standard faceted-search semantics).
 *
 * <p>Filtering is in-memory: load active products, prune, sort, paginate. Fine
 * for tenants up to thousands of products; larger tenants need indexed columns
 * and a real query plan.
 */
@Service
public class PublicProductService {

    public static final String ATTRIBUTE_PARAM_PREFIX = "attribute.";

    private final ProductDao productDao;
    private final CategoryDao categoryDao;
    private final AttributeDefinitionDao attributeDefinitionDao;

    public PublicProductService(ProductDao productDao,
                                CategoryDao categoryDao,
                                AttributeDefinitionDao attributeDefinitionDao) {
        this.productDao = productDao;
        this.categoryDao = categoryDao;
        this.attributeDefinitionDao = attributeDefinitionDao;
    }

    /**
     * Single entry point for both {@code GET /public/products} and
     * {@code POST /public/products/search}. The GET controller builds a
     * {@link ProductListRequest} from query parameters; the POST controller
     * deserializes the body directly.
     */
    @Transactional(readOnly = true)
    public PageResponse<Product> list(ProductListRequest req) {
        ProductListRequest r = req == null ? new ProductListRequest() : req;
        Pageable pageable = buildPageable(r);
        Map<String, List<String>> attributeFilters = r.getAttributes() == null
                ? Map.of() : r.getAttributes();

        List<Product> filtered = productDao.findAll().stream()
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .filter(p -> r.getCategoryId() == null
                        || (p.getCategory() != null && r.getCategoryId().equals(p.getCategory().getId())))
                .filter(p -> r.getTagIds() == null || r.getTagIds().isEmpty() || matchesAnyTag(p, r.getTagIds()))
                .filter(p -> matchesText(p, r.getQ()))
                .filter(p -> r.getCurrency() == null || r.getCurrency().equals(p.getCurrency()))
                .filter(p -> withinPrice(p, r.getMinPrice(), r.getMaxPrice()))
                .filter(p -> matchesAttributes(p, attributeFilters))
                .sorted(productSorter(pageable))
                .toList();

        int total = filtered.size();
        int from = (int) Math.min(pageable.getOffset(), total);
        int to = Math.min(from + pageable.getPageSize(), total);
        List<Product> pageContent = filtered.subList(from, to);

        Page<Product> page = new PageImpl<>(pageContent, pageable, total);
        return PageResponse.of(page);
    }

    /** Builds a Pageable from the DTO, applying defaults + caps consistently with the GET shim. */
    private static Pageable buildPageable(ProductListRequest r) {
        int page = r.getPage() != null && r.getPage() >= 0 ? r.getPage() : 0;
        int size = r.getSize() != null && r.getSize() > 0
                ? Math.min(r.getSize(), 200) : 20;
        String sortField = r.getSort() == null || r.getSort().isBlank() ? "id" : r.getSort();
        Sort.Direction dir = Sort.Direction.DESC;
        if (r.getSortDir() != null) {
            try {
                dir = Sort.Direction.fromString(r.getSortDir());
            } catch (IllegalArgumentException ignored) { }
        }
        return PageRequest.of(page, size, Sort.by(dir, sortField));
    }

    /**
     * Helper used by the GET shim: extracts {@code attribute.<Name>=<value>}
     * query parameters into a {@code Map<String, List<String>>} keyed by
     * attribute name.
     */
    public static Map<String, List<String>> extractAttributesFromQueryParams(
            MultiValueMap<String, String> params) {
        return extractAttributeFilters(params);
    }

    @Transactional(readOnly = true)
    public Product getActiveById(UUID id) {
        Product p = productDao.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id));
        if (p.getStatus() != ProductStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id);
        }
        return p;
    }

    /**
     * Returns the filter dimensions available for a category (or globally), used
     * by the storefront to render checkbox / slider filter UI dynamically.
     */
    @Transactional(readOnly = true)
    public ProductFiltersResponse filters(UUID categoryId) {
        List<AttributeDefinition> attrs;
        UUID echoedCategoryId = null;
        if (categoryId != null) {
            Category c = categoryDao.findById(categoryId).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found: " + categoryId));
            attrs = c.getAttributeDefinitions() == null ? List.of() : c.getAttributeDefinitions();
            echoedCategoryId = c.getId();
        } else {
            // No category — return all definitions (mostly useful for global "browse all" pages).
            attrs = attributeDefinitionDao.findAll();
        }

        // Derive price range and currencies from the current active product set in this category.
        List<Product> products = productDao.findAll().stream()
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .filter(p -> categoryId == null
                        || (p.getCategory() != null && categoryId.equals(p.getCategory().getId())))
                .toList();

        Set<Currency> currencies = new HashSet<>();
        Map<Currency, BigDecimal> minByCurrency = new HashMap<>();
        Map<Currency, BigDecimal> maxByCurrency = new HashMap<>();
        for (Product p : products) {
            Currency c = p.getCurrency();
            BigDecimal price = p.getBasePrice();
            if (c == null || price == null) continue;
            currencies.add(c);
            minByCurrency.merge(c, price, BigDecimal::min);
            maxByCurrency.merge(c, price, BigDecimal::max);
        }

        ProductFiltersResponse.PriceRange priceRange = null;
        // Surface a single price range only when there's a single currency in scope; otherwise
        // the storefront should display per-currency ranges separately. (V1 keeps the response
        // simple — one PriceRange or null.)
        if (currencies.size() == 1) {
            Currency only = currencies.iterator().next();
            priceRange = new ProductFiltersResponse.PriceRange(
                    minByCurrency.get(only), maxByCurrency.get(only), only);
        }

        return new ProductFiltersResponse(
                echoedCategoryId,
                attrs,
                currencies.stream().sorted(Comparator.comparing(Currency::name)).toList(),
                priceRange);
    }

    // -------- filter helpers --------

    private static Map<String, List<String>> extractAttributeFilters(MultiValueMap<String, String> params) {
        if (params == null || params.isEmpty()) return Map.of();
        Map<String, List<String>> out = new HashMap<>();
        for (var entry : params.entrySet()) {
            String key = entry.getKey();
            if (key != null && key.startsWith(ATTRIBUTE_PARAM_PREFIX)) {
                String attrName = key.substring(ATTRIBUTE_PARAM_PREFIX.length());
                if (attrName.isBlank()) continue;
                out.put(attrName, entry.getValue() == null ? List.of() : entry.getValue());
            }
        }
        return out;
    }

    /**
     * Tag matching uses OR-within-key semantics: a product matches when it has
     * <em>any</em> of the requested tags. This aligns with the storefront
     * faceted-search convention surfaced by {@code /filter-map} as
     * {@code multiValue: true} (multi-value-within-the-same-key is OR).
     */
    private static boolean matchesAnyTag(Product p, List<UUID> wanted) {
        if (p.getTags() == null || p.getTags().isEmpty()) return false;
        Set<UUID> have = new HashSet<>();
        for (Tag t : p.getTags()) if (t != null && t.getId() != null) have.add(t.getId());
        for (UUID id : wanted) if (id != null && have.contains(id)) return true;
        return false;
    }

    private static boolean matchesText(Product p, String q) {
        if (q == null || q.isBlank()) return true;
        String needle = q.toLowerCase(Locale.ROOT);
        return contains(p.getName(), needle) || contains(p.getDescription(), needle);
    }

    private static boolean contains(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static boolean withinPrice(Product p, BigDecimal min, BigDecimal max) {
        BigDecimal price = p.getBasePrice();
        if (price == null) return min == null && max == null;
        if (min != null && price.compareTo(min) < 0) return false;
        if (max != null && price.compareTo(max) > 0) return false;
        return true;
    }

    private static boolean matchesAttributes(Product p, Map<String, List<String>> filters) {
        if (filters.isEmpty()) return true;
        for (var entry : filters.entrySet()) {
            String defName = entry.getKey();
            List<String> wanted = entry.getValue();
            if (wanted == null || wanted.isEmpty()) continue;
            if (!productMatchesAttribute(p, defName, wanted)) return false;
        }
        return true;
    }

    /** AND across attributes, OR within a single attribute's values. */
    private static boolean productMatchesAttribute(Product p, String defName, List<String> wanted) {
        // Product-level attributes
        if (p.getAttributes() != null) {
            for (ProductAttribute pa : p.getAttributes()) {
                if (definitionMatches(pa.getAttributeDefinition(), defName)
                        && attributeValueMatches(pa.getAttributeValue(), wanted)) {
                    return true;
                }
            }
        }
        // Variant-level attributes
        if (p.getVariants() != null) {
            for (ProductVariant v : p.getVariants()) {
                if (v.getAttributeValues() == null) continue;
                for (ProductVariantAttribute pva : v.getAttributeValues()) {
                    if (definitionMatches(pva.getAttributeDefinition(), defName)
                            && attributeValueMatches(pva.getAttributeValue(), wanted)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean definitionMatches(AttributeDefinition def, String wantedName) {
        return def != null && def.getName() != null
                && def.getName().equalsIgnoreCase(wantedName);
    }

    private static boolean attributeValueMatches(AttributeValue v, List<String> wanted) {
        if (v == null) return false;
        Set<String> wantedSet = lowercase(wanted);
        // SELECT
        AttributeOption sel = v.getSelectValue();
        if (sel != null && sel.getValue() != null
                && wantedSet.contains(sel.getValue().toLowerCase(Locale.ROOT))) {
            return true;
        }
        // MULTI_SELECT
        if (v.getMultiSelectValues() != null) {
            for (AttributeOption opt : v.getMultiSelectValues()) {
                if (opt != null && opt.getValue() != null
                        && wantedSet.contains(opt.getValue().toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        // TEXT
        if (v.getTextValue() != null
                && wantedSet.contains(v.getTextValue().toLowerCase(Locale.ROOT))) {
            return true;
        }
        // BOOLEAN
        if (v.getBooleanValue() != null
                && wantedSet.contains(String.valueOf(v.getBooleanValue()))) {
            return true;
        }
        // NUMBER (exact match — range filters go through minPrice/maxPrice on top-level params)
        if (v.getNumberValue() != null) {
            String num = v.getNumberValue().toPlainString();
            if (wanted.contains(num)) return true;
        }
        // DATE (ISO local date, e.g. "2026-01-15")
        var dv = v.getDateValue();
        if (dv != null) {
            for (String w : wanted) {
                LocalDate ld = parseIsoDate(w);
                if (ld == null) continue;
                if (Objects.equals(dv.getYear(), ld.getYear())
                        && Objects.equals(dv.getMonth(), ld.getMonthValue())
                        && Objects.equals(dv.getDay(), ld.getDayOfMonth())) {
                    return true;
                }
            }
        }
        // TIME (ISO local time, e.g. "14:30:00")
        var tv = v.getTimeValue();
        if (tv != null) {
            for (String w : wanted) {
                LocalTime lt = parseIsoTime(w);
                if (lt == null) continue;
                if (Objects.equals(tv.getHour(), lt.getHour())
                        && Objects.equals(tv.getMinute(), lt.getMinute())
                        && Objects.equals(tv.getSecond(), lt.getSecond())) {
                    return true;
                }
            }
        }
        // DATE_TIME (ISO local datetime or Instant, e.g. "2026-01-15T14:30:00" or "...Z")
        var dtv = v.getDateTimeValue();
        if (dtv != null) {
            for (String w : wanted) {
                LocalDateTime ldt = parseIsoDateTime(w);
                if (ldt == null) continue;
                if (Objects.equals(dtv.getYear(), ldt.getYear())
                        && Objects.equals(dtv.getMonth(), ldt.getMonthValue())
                        && Objects.equals(dtv.getDay(), ldt.getDayOfMonth())
                        && Objects.equals(dtv.getHour(), ldt.getHour())
                        && Objects.equals(dtv.getMinute(), ldt.getMinute())
                        && Objects.equals(dtv.getSecond(), ldt.getSecond())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static LocalDate parseIsoDate(String s) {
        if (s == null) return null;
        try { return LocalDate.parse(s); } catch (Exception ignored) { return null; }
    }

    private static LocalTime parseIsoTime(String s) {
        if (s == null) return null;
        try { return LocalTime.parse(s); } catch (Exception ignored) { return null; }
    }

    private static LocalDateTime parseIsoDateTime(String s) {
        if (s == null) return null;
        try { return LocalDateTime.parse(s); } catch (Exception ignored) {}
        try {
            return LocalDateTime.ofInstant(Instant.parse(s), ZoneOffset.UTC);
        } catch (Exception ignored) { return null; }
    }

    private static Set<String> lowercase(Collection<String> in) {
        Set<String> out = new HashSet<>();
        if (in != null) {
            for (String s : in) if (s != null) out.add(s.toLowerCase(Locale.ROOT));
        }
        return out;
    }

    private Comparator<Product> productSorter(Pageable pageable) {
        // Default: newest first. Honor a few common Pageable sort fields if provided.
        if (pageable == null || pageable.getSort().isUnsorted()) {
            return Comparator.<Product, java.util.UUID>comparing(Product::getId,
                    Comparator.nullsLast(Comparator.reverseOrder())).reversed();
        }
        Comparator<Product> cmp = Comparator.comparing(p -> 0); // identity
        for (var order : pageable.getSort()) {
            Comparator<Product> step = switch (order.getProperty()) {
                case "basePrice" -> Comparator.comparing(Product::getBasePrice,
                        Comparator.nullsLast(Comparator.naturalOrder()));
                case "name" -> Comparator.comparing(Product::getName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                case "id" -> Comparator.comparing(Product::getId,
                        Comparator.nullsLast(Comparator.naturalOrder()));
                default -> null;
            };
            if (step == null) continue;
            cmp = cmp.thenComparing(order.isDescending() ? step.reversed() : step);
        }
        return cmp;
    }

    // Suppress unused-import noise for the rare path:
    @SuppressWarnings("unused")
    private static List<Product> unusedHelperForCompiler(ArrayList<Product> p) { return p; }
}
