package com.viescloud.llc.venzora.model.product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.viescloud.eco.viesspringutils.interfaces.annotation.GeneratedUuidV7;
import com.viescloud.eco.viesspringutils.model.TrackedTimeStamp;
import com.viescloud.llc.venzora.model.product.type.ProductVariantStatus;
import com.viescloud.llc.venzora.model.product.type.VariantPriceMode;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class ProductVariant extends TrackedTimeStamp {
    @Id
    @GeneratedUuidV7
    private UUID id;
    
    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.REFRESH, CascadeType.DETACH})
    @JoinColumn(name = "product_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Product product;
    
    @Column(columnDefinition = "TEXT", unique = true, nullable = false)
    private String sku;
    
    @Column(columnDefinition = "TEXT")
    private String variantName; // e.g., "Small Red T-Shirt"
    
    @Column()
    private BigDecimal price;

    /**
     * How {@link #price} should be interpreted relative to
     * {@code product.basePrice}. See {@link VariantPriceMode} for the modes.
     * Defaults to {@link VariantPriceMode#NORMAL} on new variants.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VariantPriceMode priceMode = VariantPriceMode.NORMAL;

    @Column()
    private Long stockQuantity;
    
    @Column()
    private BigDecimal weight;
    
    @Enumerated(EnumType.STRING)
    private ProductVariantStatus status;

    @OneToMany(mappedBy = "productVariant", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<ProductMedia> medias = new HashSet<>();

    // Variant-specific attribute values
    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<ProductVariantAttribute> attributeValues = new ArrayList<>();

    /**
     * Set the back-reference on every owned child before Hibernate cascades the
     * write. Same rationale as {@link Product#syncChildBackRefs()} — the
     * child-side {@code @JsonIgnore} back-references mean incoming JSON never
     * carries them, so we have to wire them up server-side.
     *
     * <p>For {@link ProductMedia}, we also clear the sibling {@code product}
     * slot so a media that lives on a variant cannot look like it also lives on
     * the product.
     */
    @PrePersist
    @PreUpdate
    private void syncChildBackRefs() {
        // Defensive: never let priceMode round-trip as null. Clients that omit
        // the field, or send {"priceMode": null}, default to NORMAL.
        if (priceMode == null) priceMode = VariantPriceMode.NORMAL;

        if (medias != null) {
            for (ProductMedia m : medias) {
                if (m == null) continue;
                m.setProductVariant(this);
                m.setProduct(null);
            }
            ProductMedia.normalizePrimary(medias);
        }
        if (attributeValues != null) {
            for (ProductVariantAttribute a : attributeValues) {
                if (a != null) a.setVariant(this);
            }
        }
    }

    /**
     * The resolved price the shopper should actually pay for this variant.
     *
     * <ul>
     *   <li>{@link VariantPriceMode#NORMAL NORMAL} → returns {@link #price} directly.</li>
     *   <li>{@link VariantPriceMode#FLAT_ADJUSTMENT FLAT_ADJUSTMENT} → returns
     *       {@code product.basePrice + price}. Positive = markup, negative = discount.</li>
     *   <li>{@link VariantPriceMode#PERCENT_ADJUSTMENT PERCENT_ADJUSTMENT} → returns
     *       {@code product.basePrice × (1 + price/100)}, rounded to 2 decimals HALF_UP.</li>
     * </ul>
     *
     * If the product back-reference or its {@code basePrice} is missing (e.g. a
     * detached variant read out of context), falls back to the raw {@link #price}.
     * If {@link #price} is null on an adjustment mode, the delta is treated as 0.
     *
     * <p>Computed on read; exposed as {@code effectivePrice} in JSON. Not persisted.
     */
    public BigDecimal getEffectivePrice() {
        VariantPriceMode mode = priceMode != null ? priceMode : VariantPriceMode.NORMAL;
        if (mode == VariantPriceMode.NORMAL) return price;
        if (product == null || product.getBasePrice() == null) return price;

        BigDecimal base = product.getBasePrice();
        BigDecimal delta = price != null ? price : BigDecimal.ZERO;
        return switch (mode) {
            case FLAT_ADJUSTMENT -> base.add(delta).setScale(2, RoundingMode.HALF_UP);
            case PERCENT_ADJUSTMENT -> {
                BigDecimal factor = BigDecimal.ONE.add(
                        delta.divide(HUNDRED, 6, RoundingMode.HALF_UP));
                yield base.multiply(factor).setScale(2, RoundingMode.HALF_UP);
            }
            default -> price;
        };
    }

    private static final BigDecimal HUNDRED = new BigDecimal("100");
}
