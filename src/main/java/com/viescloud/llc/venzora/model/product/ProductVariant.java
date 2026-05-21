package com.viescloud.llc.venzora.model.product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.viescloud.eco.viesspringutils.model.TrackedTimeStamp;
import com.viescloud.llc.venzora.model.product.type.ProductVariantStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class ProductVariant extends TrackedTimeStamp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.REFRESH, CascadeType.DETACH})
    @JoinColumn(name = "product_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Product product;
    
    @Column(columnDefinition = "TEXT", unique = true, nullable = false)
    private String sku;
    
    @Column(columnDefinition = "TEXT")
    private String variantName; // e.g., "Small Red T-Shirt"
    
    @Column()
    private BigDecimal price;
    
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
}
