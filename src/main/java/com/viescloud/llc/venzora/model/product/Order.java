package com.viescloud.llc.venzora.model.product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.viescloud.eco.viesspringutils.model.TrackedTimeStampUserAccess;
import com.viescloud.llc.venzora.model.address.Address;
import com.viescloud.llc.venzora.model.product.type.OrderStatus;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class Order extends TrackedTimeStampUserAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    @Column(nullable = false)
    private Long userId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Column(nullable = false)
    private BigDecimal tax;

    @Column(nullable = false)
    private BigDecimal shippingCost;

    @Column(nullable = false)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "shipping_street", columnDefinition = "TEXT")),
        @AttributeOverride(name = "suite", column = @Column(name = "shipping_suite", columnDefinition = "TEXT")),
        @AttributeOverride(name = "city", column = @Column(name = "shipping_city", columnDefinition = "TEXT")),
        @AttributeOverride(name = "state", column = @Column(name = "shipping_state", columnDefinition = "TEXT")),
        @AttributeOverride(name = "postalCode", column = @Column(name = "shipping_postal_code", columnDefinition = "TEXT")),
        @AttributeOverride(name = "country", column = @Column(name = "shipping_country", columnDefinition = "TEXT")),
        @AttributeOverride(name = "type", column = @Column(name = "shipping_type"))
    })
    private Address shippingAddress;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "billing_street", columnDefinition = "TEXT")),
        @AttributeOverride(name = "suite", column = @Column(name = "billing_suite", columnDefinition = "TEXT")),
        @AttributeOverride(name = "city", column = @Column(name = "billing_city", columnDefinition = "TEXT")),
        @AttributeOverride(name = "state", column = @Column(name = "billing_state", columnDefinition = "TEXT")),
        @AttributeOverride(name = "postalCode", column = @Column(name = "billing_postal_code", columnDefinition = "TEXT")),
        @AttributeOverride(name = "country", column = @Column(name = "billing_country", columnDefinition = "TEXT")),
        @AttributeOverride(name = "type", column = @Column(name = "billing_type"))
    })
    private Address billingAddress;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
