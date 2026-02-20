package com.viescloud.llc.venzora.model.product;

import java.math.BigDecimal;

import com.viescloud.eco.viesspringutils.config.jpa.DateTimeConverter;
import com.viescloud.eco.viesspringutils.model.TrackedTimeStamp;
import com.viescloud.eco.viesspringutils.util.DateTime;
import com.viescloud.llc.venzora.model.product.type.DiscountType;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Discount extends TrackedTimeStamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column
    private Integer maxUses; // Null means unlimited

    @Column(nullable = false)
    private Integer currentUses = 0;

    @Column(nullable = false)
    private Boolean active = true;
}
