package com.viescloud.llc.venzora.model.product;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The complete set of filters the storefront may render against {@code GET
 * /public/products}. Computed by scanning categories, tags, attribute
 * definitions, and the active product set; refreshed every 60 seconds by a
 * background task.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FilterMap {

    private Instant computedAt;
    private List<FilterSpec> filters;
}
