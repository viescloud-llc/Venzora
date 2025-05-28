package com.viescloud.llc.venzora.model.product;

import com.viescloud.eco.viesspringutils.util.DateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Payment {
    
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
    private Double amount;

    @Embedded
    private DateTime paymentDate;
}
