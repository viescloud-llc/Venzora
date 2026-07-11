package com.viescloud.llc.venzora.model.product.type;

/**
 * How to interpret {@code ProductVariant.price} relative to the parent
 * {@code Product.basePrice}.
 *
 * <p>Use {@code ProductVariant.getEffectivePrice()} to resolve the final price
 * — the mode logic is centralized there so callers never need to branch.
 */
public enum VariantPriceMode {

    /** {@code price} is the effective price as-is. Ignores {@code basePrice}. Default. */
    NORMAL,

    /**
     * {@code price} is a signed delta applied to {@code basePrice}.
     * <p>Example: {@code basePrice=100}, {@code price=+10} → effective {@code 110};
     * {@code price=-10} → effective {@code 90}.
     */
    FLAT_ADJUSTMENT,

    /**
     * {@code price} is a signed percentage applied to {@code basePrice}.
     * <p>Example: {@code basePrice=100}, {@code price=+10} → effective {@code 110}
     * (a 10% markup); {@code price=-10} → effective {@code 90} (a 10% discount).
     * <p>Computed as {@code basePrice * (1 + price/100)} then rounded to 2 decimals
     * using {@code HALF_UP}.
     */
    PERCENT_ADJUSTMENT
}
