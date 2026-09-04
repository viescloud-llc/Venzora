package com.viescloud.llc.venzora.model.product;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.viescloud.eco.viesspringutils.config.jpa.DateTimeConverter;
import com.viescloud.eco.viesspringutils.interfaces.annotation.GeneratedUuidV7;
import com.viescloud.eco.viesspringutils.model.TrackedTimeStamp;
import com.viescloud.eco.viesspringutils.util.DateTime;
import com.viescloud.llc.venzora.model.product.type.DiscountType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Discount extends TrackedTimeStamp {

    @Id
    @GeneratedUuidV7
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @Column(nullable = false)
    private BigDecimal discountValue; // Percentage or fixed amount value

    @Column
    private BigDecimal minimumOrderAmount;

    @Column
    private BigDecimal maximumDiscountAmount;

    @Column(columnDefinition = "TEXT", nullable = false)
    @Convert(converter = DateTimeConverter.class)
    private DateTime validFrom;

    @Column(columnDefinition = "TEXT", nullable = false)
    @Convert(converter = DateTimeConverter.class)
    private DateTime validTo;

    /** Null OR zero means unlimited (the Manager sends 0 for "no limit"). */
    @Column
    private Integer maxUses;

    /** Bumped once per checkout START (an abandoned PayPal approval still consumes a use). Server-owned. */
    @Column(nullable = false)
    private Integer currentUses = 0;

    @Column(nullable = false)
    private Boolean active = true;

    // ---- Product matchers (empty = whole cart; set = only lines whose product has ANY of them) ----

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.REFRESH, CascadeType.DETACH})
    @JoinTable(name = "discount_tags")
    private Set<Tag> tags = new HashSet<>();

    /** Matches the product's category or any descendant of the picked categories. */
    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.REFRESH, CascadeType.DETACH})
    @JoinTable(name = "discount_categories")
    private Set<Category> categories = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.REFRESH, CascadeType.DETACH})
    @JoinTable(name = "discount_attribute_definitions")
    private Set<AttributeDefinition> attributeDefinitions = new HashSet<>();

    /** True when the discount is scoped to matching lines rather than the whole cart. */
    public boolean hasProductMatchers() {
        return com.viescloud.llc.venzora.service.product.ProductMatching.hasMatchers(tags, categories, attributeDefinitions);
    }

    public boolean matchesProduct(com.viescloud.llc.venzora.service.product.ProductMatching.ProductContext ctx) {
        return com.viescloud.llc.venzora.service.product.ProductMatching.matches(tags, categories, attributeDefinitions, ctx);
    }

    /** Usage cap semantics: null or <= 0 = unlimited. */
    public boolean isUnlimited() {
        return maxUses == null || maxUses <= 0;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Discount other && id != null && Objects.equals(id, other.id));
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : id.hashCode();
    }
}
