package com.viescloud.llc.venzora.model.product;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.viescloud.eco.viesspringutils.interfaces.annotation.GeneratedUuidV7;
import com.viescloud.eco.viesspringutils.model.TrackedTimeStamp;
import com.viescloud.llc.venzora.model.product.type.ProductStatus;
import com.viescloud.llc.venzora.model.share_enum.Currency;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Product extends TrackedTimeStamp {
    
    @Id
    @GeneratedUuidV7
    private UUID id;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.REFRESH, CascadeType.DETACH}, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Currency currency;
    
    @Column(nullable = false)
    private BigDecimal basePrice;

    @Column(columnDefinition = "TEXT")
    private String baseSku;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.REFRESH, CascadeType.DETACH})
    private Set<Tag> tags = new HashSet<>();

    // One product can have multiple variants
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<ProductVariant> variants = new HashSet<>();
    
    // Dynamic attributes for the base product
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<ProductAttribute> attributes = new HashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<ProductMedia> medias = new HashSet<>();

    /**
     * Set the back-reference on every owned child (and every grandchild reached
     * through a variant) before Hibernate cascades the write.
     *
     * <p><b>Why recursive.</b> {@code ProductVariant.medias} and
     * {@code ProductVariant.attributeValues} are {@code mappedBy} — i.e. inverse
     * collections. Adding an item to an inverse collection <em>does not</em>
     * make the owning entity (the variant) dirty, so Hibernate won't schedule
     * an {@code UPDATE} on the variant and {@link ProductVariant#syncChildBackRefs}
     * won't fire. We therefore cannot rely on the variant's own callback here
     * — the walk has to be done from the parent whose write is definitely
     * happening: {@code Product}.
     *
     * <p><b>Ordering.</b> Variants are walked <em>after</em> product-level
     * medias. If (by client error) the same media instance appears in both
     * {@code product.medias} and {@code variant.medias}, the variant-level
     * assignment wins — variant scope is the more specific ownership.
     */
    @PrePersist
    @PreUpdate
    private void syncChildBackRefs() {
        if (attributes != null) {
            for (ProductAttribute a : attributes) {
                if (a != null) a.setProduct(this);
            }
        }
        if (medias != null) {
            for (ProductMedia m : medias) {
                if (m == null) continue;
                m.setProduct(this);
                m.setProductVariant(null);
            }
            ProductMedia.normalizePrimary(medias);
        }
        if (variants != null) {
            for (ProductVariant v : variants) {
                if (v == null) continue;
                v.setProduct(this);
                if (v.getMedias() != null) {
                    for (ProductMedia m : v.getMedias()) {
                        if (m == null) continue;
                        m.setProductVariant(v);
                        m.setProduct(null);
                    }
                    ProductMedia.normalizePrimary(v.getMedias());
                }
                if (v.getAttributeValues() != null) {
                    for (ProductVariantAttribute a : v.getAttributeValues()) {
                        if (a != null) a.setVariant(v);
                    }
                }
            }
        }
    }
}
