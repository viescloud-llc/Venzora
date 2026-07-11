package com.viescloud.llc.venzora.model.product;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Self-describing filter spec for one storefront query parameter. The frontend
 * iterates a list of these to render filter UI without baking the catalog's
 * schema into the build.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FilterSpec {

    /** Query-param key the storefront should use on {@code GET /public/products}. */
    private String key;

    /** For ranges, the "max" param name (e.g. {@code "maxPrice"}). Null otherwise. */
    private String secondaryKey;

    /** Human-readable label for the filter UI. */
    private String displayName;

    /** How to render: text, number, range, single-select, multi-select, etc. */
    private FilterKind kind;

    /** Whether the param can be repeated to OR multiple values within this key. */
    private Boolean multiValue;

    /** Populated for SELECT-typed filters; null otherwise. */
    private List<FilterOption> options;

    /**
     * For RANGE_PRICE: keys are currency codes ({@code "USD"}, {@code "EUR"}, …).
     * For RANGE_NUMBER: keys are unit strings or {@code ""} when none.
     * Null when not a range.
     */
    private Map<String, FilterRange> ranges;

    /** Free-form additional context (attribute.type, attribute.unit, …). */
    private Map<String, String> meta;
}
