package com.viescloud.llc.venzora.model.product;

import java.util.HashSet;
import java.util.Set;

import com.viescloud.eco.viesspringutils.config.jpa.BooleanConverter;
import com.viescloud.eco.viesspringutils.util.DateTime;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Product {
    
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

    @Column(columnDefinition = "TEXT")
    @Convert(converter = BooleanConverter.class)
    private Boolean active;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "year", column = @Column(name = "created_year")),
        @AttributeOverride(name = "month", column = @Column(name = "created_month")),
        @AttributeOverride(name = "day", column = @Column(name = "created_day")),
        @AttributeOverride(name = "hour", column = @Column(name = "created_hour")),
        @AttributeOverride(name = "minute", column = @Column(name = "created_minute")),
        @AttributeOverride(name = "second", column = @Column(name = "created_second")),
        @AttributeOverride(name = "millis", column = @Column(name = "created_millis")),
        @AttributeOverride(name = "currentZoneId", column = @Column(name = "created_currentZoneId")),
        @AttributeOverride(name = "bypassMax", column = @Column(name = "created_bypassMax"))
    })
    private DateTime createdAt;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "year", column = @Column(name = "updated_year")),
        @AttributeOverride(name = "month", column = @Column(name = "updated_month")),
        @AttributeOverride(name = "day", column = @Column(name = "updated_day")),
        @AttributeOverride(name = "hour", column = @Column(name = "updated_hour")),
        @AttributeOverride(name = "minute", column = @Column(name = "updated_minute")),
        @AttributeOverride(name = "second", column = @Column(name = "updated_second")),
        @AttributeOverride(name = "millis", column = @Column(name = "updated_millis")),
        @AttributeOverride(name = "currentZoneId", column = @Column(name = "updated_currentZoneId")),
        @AttributeOverride(name = "bypassMax", column = @Column(name = "updated_bypassMax"))
    })
    private DateTime updatedAt;
}
