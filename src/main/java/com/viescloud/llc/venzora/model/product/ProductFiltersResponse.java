package com.viescloud.llc.venzora.model.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.viescloud.llc.venzora.model.share_enum.Currency;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Filter discovery payload for the public storefront. The frontend uses this to
 * render dynamic filter checkboxes / sliders without knowing the catalog's
 * attribute schema ahead of time.
 *
 * <p>When a {@code categoryId} is provided, {@link #attributes} is the category's
 * {@code attributeDefinitions} list. When no category is given, it falls back to
 * the global set of variant-level definitions across all categories.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductFiltersResponse {

    private UUID categoryId;             // echoed back, null when no category was requested
    private List<AttributeDefinition> attributes;
    private List<Currency> currencies;   // currencies present in the matching product set
    private PriceRange priceRange;       // null when no products matched

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PriceRange {
        private BigDecimal min;
        private BigDecimal max;
        private Currency currency;
    }
}
