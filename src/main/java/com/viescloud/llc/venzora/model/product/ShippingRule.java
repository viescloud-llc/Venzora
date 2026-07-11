package com.viescloud.llc.venzora.model.product;

import java.math.BigDecimal;
import java.util.UUID;

import com.viescloud.eco.viesspringutils.interfaces.annotation.GeneratedUuidV7;
import com.viescloud.eco.viesspringutils.model.TrackedTimeStamp;
import com.viescloud.llc.venzora.model.share_enum.Currency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Shipping cost rule, one per currency. The checkout orchestrator looks up a
 * rule by the cart's currency and applies {@link #flatFee}, unless the cart
 * subtotal meets or exceeds {@link #freeAboveAmount} in which case shipping is
 * free. If no active rule exists for a given currency, shipping is treated as
 * zero (and a warning is logged).
 */
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class ShippingRule extends TrackedTimeStamp {

    @Id
    @GeneratedUuidV7
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private Currency currency;

    @Column(nullable = false)
    private BigDecimal flatFee;

    /** Cart subtotal at or above which shipping is waived. Null = no free-shipping threshold. */
    @Column
    private BigDecimal freeAboveAmount;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Boolean active = true;
}
