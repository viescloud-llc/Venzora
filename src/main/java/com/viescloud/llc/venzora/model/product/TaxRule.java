package com.viescloud.llc.venzora.model.product;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.viescloud.eco.viesspringutils.interfaces.annotation.GeneratedUuidV7;
import com.viescloud.eco.viesspringutils.model.TrackedTimeStamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * A user-defined sales-tax / VAT rule. The {@code country}, {@code state},
 * {@code city}, and {@code postalCode} fields are <strong>matchers</strong> —
 * each one that is non-null must equal the corresponding part of the shipping
 * address (case-insensitive for text fields). A rule with all four matchers
 * null is the implicit default and applies whenever no more-specific rule
 * matches.
 *
 * <p>When multiple rules match an address, the matcher with more non-null
 * fields wins ("specificity"); {@link #priority} breaks ties (higher first).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class TaxRule extends TrackedTimeStamp {

    @Id
    @GeneratedUuidV7
    private UUID id;

    @Column(nullable = false)
    private String name;

    /** Tax rate as a percentage. {@code 8.00} means 8 percent. */
    @Column(nullable = false)
    private BigDecimal rate;

    /** ISO 3166-1 alpha-2 country code (e.g. "US", "DE"). Null matches any country. */
    @Column
    private String country;

    /** State / region / province code (e.g. "NY", "CA", "BY"). Null matches any state. */
    @Column
    private String state;

    /** City name. Case-insensitive match. Null matches any city. */
    @Column
    private String city;

    /** Postal code (exact match). Null matches any postal code. */
    @Column
    private String postalCode;

    /** Tiebreaker for rules of equal specificity. Higher wins. */
    @Column(nullable = false)
    private Integer priority = 0;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(columnDefinition = "TEXT")
    private String description;
}
