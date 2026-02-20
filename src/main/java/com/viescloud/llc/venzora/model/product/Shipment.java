package com.viescloud.llc.venzora.model.product;

import com.viescloud.eco.viesspringutils.config.jpa.DateTimeConverter;
import com.viescloud.eco.viesspringutils.model.TrackedTimeStamp;
import com.viescloud.eco.viesspringutils.util.DateTime;
import com.viescloud.llc.venzora.model.product.type.ShipmentStatus;

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
import jakarta.persistence.JoinColumn;
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
public class Shipment extends TrackedTimeStamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.REFRESH, CascadeType.DETACH})
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, unique = true)
    private String trackingNumber;

    @Column(nullable = false)
    private String carrier; // UPS, FedEx, USPS, DHL, etc.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status;

    @Column(columnDefinition = "TEXT", nullable = false)
    @Convert(converter = DateTimeConverter.class)
    private DateTime estimatedDeliveryDate;

    @Column(columnDefinition = "TEXT", nullable = false)
    @Convert(converter = DateTimeConverter.class)
    private DateTime actualDeliveryDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String trackingUrl;
}
