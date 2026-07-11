package com.viescloud.llc.venzora.model.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.viescloud.llc.venzora.model.share_enum.Currency;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body for {@code POST /api/v1/public/products/search}, the preferred way to list
 * products when the query has more than a handful of filters (URL length limits
 * make the GET path awkward beyond ~10 attribute values).
 *
 * <p>Every field is optional and tolerant of nulls — only the filters the
 * frontend actually populates narrow the result set.
 *
 * <p>Filter semantics match the GET endpoint exactly:
 * <ul>
 *   <li><b>AND</b> across different fields — every populated filter must match.</li>
 *   <li><b>OR</b> within the same key — {@code tagIds: ["A","B"]} matches A or B;
 *       {@code attributes: { "Size": ["S","M"] }} matches Size=S or Size=M.</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductListRequest {

    /** Exact-match category. */
    private UUID categoryId;

    /** OR-within-key tag match — products having any of these tags. */
    private List<UUID> tagIds;

    /** Substring search on name + description, case-insensitive. */
    private String q;

    /** Exact-match currency. */
    private Currency currency;

    /** Inclusive lower bound on basePrice. */
    private BigDecimal minPrice;

    /** Inclusive upper bound on basePrice. */
    private BigDecimal maxPrice;

    /**
     * Dynamic attribute filters keyed by {@code AttributeDefinition.name}.
     * Each entry's values are OR-ed within the key; entries themselves are AND-ed.
     * Example: {@code { "Size": ["S","M"], "Color": ["Red"] }} → (Size=S OR Size=M) AND Color=Red.
     */
    private Map<String, List<String>> attributes;

    /** 0-indexed page. Defaults to 0. */
    private Integer page;

    /** Items per page. Defaults to 20, capped at 200. */
    private Integer size;

    /** Sort field. One of {@code id}, {@code name}, {@code basePrice}. Default: {@code id}. */
    private String sort;

    /** {@code ASC} or {@code DESC} (case-insensitive). Default: {@code DESC}. */
    private String sortDir;
}
