package com.viescloud.llc.venzora.model.product;

import java.util.HashSet;
import java.util.Set;

import com.viescloud.eco.viesspringutils.util.DateTime;
import com.viescloud.llc.venzora.model.address.Address;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Order {
    
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private Long productId;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false)
    private Double totalPrice;
    
    @Column(nullable = false)
    private String status;
    
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<Address> addresses = new HashSet<>();

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
