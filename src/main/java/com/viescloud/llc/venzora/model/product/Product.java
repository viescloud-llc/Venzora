package com.viescloud.llc.venzora.model.product;

import java.util.HashSet;
import java.util.Set;

import com.viescloud.eco.viesspringutils.config.jpa.BooleanConverter;
import com.viescloud.eco.viesspringutils.model.TrackedTimeStamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
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
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private Double price;
    
    @Column(nullable = false)
    private Long stockQuantity;
    
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> imageUrls = new HashSet<>();

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.REFRESH, CascadeType.DETACH}, optional = false)
    private Category category;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.REFRESH, CascadeType.DETACH})
    private Set<Tag> tags = new HashSet<>();

    @Column(length = 20)
    @Convert(converter = BooleanConverter.class)
    private Boolean active;
}
