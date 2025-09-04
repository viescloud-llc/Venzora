package com.viescloud.llc.venzora.model.product;

import java.math.BigDecimal;

import com.viescloud.eco.viesspringutils.model.TrackedTimeStamp;
import com.viescloud.llc.venzora.model.product.type.PaymentMethodtype;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
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
public class Payment extends TrackedTimeStamp {
    
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Enumerated(jakarta.persistence.EnumType.STRING)
    private PaymentMethodtype paymentMethod;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String paymentStatus;

    @Column(nullable = false)
    private BigDecimal amount;
}
