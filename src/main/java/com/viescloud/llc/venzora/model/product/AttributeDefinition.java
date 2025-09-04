package com.viescloud.llc.venzora.model.product;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.viescloud.eco.viesspringutils.config.jpa.BooleanConverter;
import com.viescloud.llc.venzora.model.product.type.ProductAttributeType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttributeDefinition implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", unique = true, nullable = false)
    private String name; // e.g., "Size", "Color", "Material"
    
    @Column(columnDefinition = "TEXT")
    private String displayName; // e.g., "Product Size"
    
    @Enumerated(EnumType.STRING)
    private ProductAttributeType type; // TEXT, NUMBER, BOOLEAN, SELECT, MULTI_SELECT
    
    @Column(columnDefinition = "TEXT")
    private String unit; // e.g., "cm", "kg", "%"
    
    @Column(columnDefinition = "TEXT")
    @Convert(converter = BooleanConverter.class)
    private Boolean required;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = BooleanConverter.class)
    private Boolean variantLevel; // true if this attribute creates variants
    
    // Predefined options for SELECT/MULTI_SELECT types
    @OneToMany(mappedBy = "attributeDefinition", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<AttributeOption> options = new ArrayList<>();
}
