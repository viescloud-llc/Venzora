package com.viescloud.llc.venzora.model.product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.viescloud.eco.viesspringutils.interfaces.annotation.GeneratedUuidV7;
import com.viescloud.eco.viesspringutils.model.TrackedTimeStampUserAccess;
import com.viescloud.llc.venzora.model.address.Address;
import com.viescloud.llc.venzora.model.product.type.FulfillmentStatus;
import com.viescloud.llc.venzora.model.share_enum.Currency;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * The fulfillment-side record of a purchase. Bridges to the vies-spring-utils
 * checkout module's {@code CheckoutOrder} (payment side) via {@link #checkoutOrderId}.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "order_fulfillments")
public class OrderFulfillment extends TrackedTimeStampUserAccess {

    @Id
    @GeneratedUuidV7
    private UUID id;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    @Column(nullable = false)
    private UUID userId;

    /**
     * Foreign-key-by-value to {@code CheckoutOrder.id} in the checkout module.
     * Plain UUID column (not @ManyToOne) so the bridge does not require
     * @EntityScan to span packages. Null until the checkout flow has created
     * the matching CheckoutOrder.
     */
    @Column
    private UUID checkoutOrderId;

    /**
     * Denormalized currency of the cart at checkout time. Stored on the order so
     * reports can group by currency without joining through items → variant → product.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    @OneToMany(mappedBy = "orderFulfillment", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<OrderFulfillmentItem> items = new ArrayList<>();

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
    private FulfillmentStatus status;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "shipping_street", columnDefinition = "TEXT")),
        @AttributeOverride(name = "suite", column = @Column(name = "shipping_suite", columnDefinition = "TEXT")),
        @AttributeOverride(name = "city", column = @Column(name = "shipping_city", columnDefinition = "TEXT")),
        @AttributeOverride(name = "state", column = @Column(name = "shipping_state", columnDefinition = "TEXT")),
        @AttributeOverride(name = "postalCode", column = @Column(name = "shipping_postal_code", columnDefinition = "TEXT")),
        @AttributeOverride(name = "country", column = @Column(name = "shipping_country", columnDefinition = "TEXT")),
        @AttributeOverride(name = "district", column = @Column(name = "shipping_district", columnDefinition = "TEXT")),
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
        @AttributeOverride(name = "district", column = @Column(name = "billing_district", columnDefinition = "TEXT")),
        @AttributeOverride(name = "type", column = @Column(name = "billing_type"))
    })
    private Address billingAddress;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Free-form snapshot bag. The checkout orchestrator writes system keys here at
     * sale time ({@code checkout.*}, {@code tax.*}, {@code discount.*},
     * {@code shipping.*}) — these are immutable history, not configuration.
     *
     * <p>Managers can also add their own free-form keys (convention: {@code notes.*})
     * to record incident context, customer-service decisions, fraud-review outcomes,
     * etc. Keys are dotted strings; values are plain text.
     *
     * <p>This bag intentionally has <strong>no FK</strong> to TaxRule, ShippingRule,
     * Discount, or CheckoutOrder so the snapshot survives later edits or deletions
     * of those rows.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "order_fulfillment_metadata",
        joinColumns = @JoinColumn(name = "order_fulfillment_id"))
    @MapKeyColumn(name = "meta_key")
    @Column(name = "meta_value", columnDefinition = "TEXT")
    private Map<String, String> metadata = new HashMap<>();

    /**
     * Set the back-reference on every owned child before Hibernate cascades the
     * write. Same rationale as {@link Product#syncChildBackRefs()} — child-side
     * {@code @JsonIgnore} back-references mean incoming JSON never carries them.
     */
    @PrePersist
    @PreUpdate
    private void syncChildBackRefs() {
        if (items != null) {
            for (OrderFulfillmentItem i : items) {
                if (i != null) i.setOrderFulfillment(this);
            }
        }
    }
}
