package com.viescloud.llc.venzora.controller.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.interfaces.annotation.PublicEndpoint;

import com.viescloud.eco.viesspringutils.model.PageResponse;
import com.viescloud.llc.venzora.model.product.FilterMap;
import com.viescloud.llc.venzora.model.product.Product;
import com.viescloud.llc.venzora.model.product.ProductFiltersResponse;
import com.viescloud.llc.venzora.model.product.ProductListRequest;
import com.viescloud.llc.venzora.model.share_enum.Currency;
import com.viescloud.llc.venzora.service.product.ProductFilterMapService;
import com.viescloud.llc.venzora.service.product.PublicProductService;

/**
 * Unauthenticated, read-only product browse endpoints for the storefront. Returns
 * only {@code ACTIVE} products. Supports faceted filtering against the dynamic
 * attribute schema in two equivalent ways:
 *
 * <ul>
 *   <li><b>{@code POST /search}</b> — preferred. Pass a {@link ProductListRequest}
 *       JSON body. Avoids URL-length limits when many filters are stacked.</li>
 *   <li><b>{@code GET /}</b> — same filters via query params. Convenient for
 *       bookmarkable URLs and quick testing. Subject to URL-length limits at
 *       proxies / browsers.</li>
 * </ul>
 *
 * <p>Both routes go through the same service entry point and produce identical
 * results. Filter semantics: AND across different keys, OR within the same key.
 */
@PublicEndpoint("Unauthenticated storefront catalog reads (ACTIVE products only)")
@RestController
@RequestMapping("/api/v1/public/products")
public class PublicProductController {

    private final PublicProductService service;
    private final ProductFilterMapService filterMapService;

    public PublicProductController(PublicProductService service,
                                   ProductFilterMapService filterMapService) {
        this.service = service;
        this.filterMapService = filterMapService;
    }

    /**
     * <b>Primary</b> list endpoint — JSON body, no URL-length limits, every filter
     * dimension expressible as typed fields.
     */
    @PostMapping("/search")
    public PageResponse<Product> search(@RequestBody(required = false) ProductListRequest body) {
        return service.list(body == null ? new ProductListRequest() : body);
    }

    /**
     * Same logic as {@link #search}, but exposed via query parameters for
     * bookmarkability and simple browser / curl use. Builds a
     * {@link ProductListRequest} from the params and delegates to the same service
     * call.
     *
     * <p>Example:
     * <pre>
     * GET /api/v1/public/products?categoryId=...&attribute.Size=Small&attribute.Color=Red&page=0&size=20
     * </pre>
     *
     * <p>Supported sort fields (via the {@code sort} query param): {@code basePrice},
     * {@code name}, {@code id}. Default: newest-first (descending by UUIDv7 id).
     */
    @GetMapping
    public PageResponse<Product> list(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) List<UUID> tagIds,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Currency currency,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam MultiValueMap<String, String> allParams) {
        ProductListRequest req = new ProductListRequest();
        req.setCategoryId(categoryId);
        req.setTagIds(tagIds);
        req.setQ(q);
        req.setCurrency(currency);
        req.setMinPrice(minPrice);
        req.setMaxPrice(maxPrice);
        req.setAttributes(PublicProductService.extractAttributesFromQueryParams(allParams));
        req.setPage(page);
        req.setSize(size);
        req.setSort(sort);
        req.setSortDir(sortDir);
        return service.list(req);
    }

    /** Single active product; 404 if missing or non-ACTIVE. */
    @GetMapping("/{id}")
    public Product getById(@PathVariable UUID id) {
        return service.getActiveById(id);
    }

    /**
     * Discover the filter dimensions for a single category (or globally if no
     * category is supplied). Lightweight, category-scoped view; computed per
     * request. For the full, globally-cached filter catalog with kind hints, see
     * {@link #filterMap()}.
     */
    @GetMapping("/filters")
    public ProductFiltersResponse filters(@RequestParam(required = false) UUID categoryId) {
        return service.filters(categoryId);
    }

    /**
     * The complete, self-describing filter catalog: every query parameter the
     * storefront may use on {@code GET /public/products}, with a {@code kind}
     * hint that tells the frontend how to render each one. Backed by a
     * 60-second refresh cache. The first request after startup may take longer
     * while the cache warms; subsequent requests are served from memory.
     */
    @GetMapping("/filter-map")
    public FilterMap filterMap() {
        return filterMapService.get();
    }
}
