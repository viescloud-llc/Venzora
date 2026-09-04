package com.viescloud.llc.venzora.model.product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.viescloud.eco.viesspringutils.interfaces.annotation.GeneratedUuidV7;
import com.viescloud.eco.viesspringutils.model.TrackedTimeStamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Convert;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import com.viescloud.eco.viesspringutils.config.jpa.StringListConverter;
import com.viescloud.llc.venzora.model.address.Address;
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

    /**
     * Alternative spellings that should ALSO match this rule's country
     * (e.g. "United States", "USA", "U.S."). Case-insensitive; addresses are
     * free text so aliases vastly improve hit rate.
     */
    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> countryAliases = new ArrayList<>();

    /** State / region / province code (e.g. "NY", "CA", "BY"). Null matches any state. */
    @Column
    private String state;

    /** Alternative spellings for the state (e.g. "New York"). Case-insensitive. */
    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> stateAliases = new ArrayList<>();

    /** District (sub-city / administrative area). Case-insensitive. Null matches any district. */
    @Column
    private String district;

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

    // ---- Product matchers (empty = match any product; non-empty = product must have ANY of them) ----

    /** Match products carrying any of these tags. */
    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.REFRESH, CascadeType.DETACH})
    @JoinTable(name = "tax_rule_tags")
    private Set<Tag> tags = new HashSet<>();

    /** Match products in any of these categories — OR any descendant category (an "Apparel" rule covers "T-Shirts"). */
    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.REFRESH, CascadeType.DETACH})
    @JoinTable(name = "tax_rule_categories")
    private Set<Category> categories = new HashSet<>();

    /** Match products (or the sold variant) carrying any of these attribute definitions. */
    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.REFRESH, CascadeType.DETACH})
    @JoinTable(name = "tax_rule_attribute_definitions")
    private Set<AttributeDefinition> attributeDefinitions = new HashSet<>();

    // ============================================================
    //  Matching — the ONE definition used by checkout and reports
    // ============================================================

    /** Location matchers that are set (country/state/city/postalCode/district). Aliases don't add specificity. */
    public int locationSpecificity() {
        int s = 0;
        if (isSet(country)) s++;
        if (isSet(state)) s++;
        if (isSet(city)) s++;
        if (isSet(postalCode)) s++;
        if (isSet(district)) s++;
        return s;
    }

    /** Product matchers that are set (tags / categories / attributeDefinitions), each counting once. */
    public int productSpecificity() {
        int s = 0;
        if (tags != null && !tags.isEmpty()) s++;
        if (categories != null && !categories.isEmpty()) s++;
        if (attributeDefinitions != null && !attributeDefinitions.isEmpty()) s++;
        return s;
    }

    /** Total specificity — the primary ordering key; {@link #getPriority()} breaks ties. */
    public int specificity() {
        return locationSpecificity() + productSpecificity();
    }

    public boolean hasProductMatchers() {
        return productSpecificity() > 0;
    }

    public boolean matchesLocation(Address address) {
        if (address == null) {
            return locationSpecificity() == 0; // a null address only matches location catch-alls
        }
        return matchesLocation(address.getCountry(), address.getState(), address.getCity(),
                address.getPostalCode(), address.getDistrict());
    }

    /** Every SET matcher must match (case-insensitive, trimmed); country/state also accept their aliases. */
    public boolean matchesLocation(String addrCountry, String addrState, String addrCity, String addrPostalCode, String addrDistrict) {
        if (isSet(country) && !matchesWithAliases(country, countryAliases, addrCountry)) return false;
        if (isSet(state) && !matchesWithAliases(state, stateAliases, addrState)) return false;
        if (isSet(city) && !eqIgnoreCase(city, addrCity)) return false;
        if (isSet(postalCode) && !eqIgnoreCase(postalCode, addrPostalCode)) return false;
        if (isSet(district) && !eqIgnoreCase(district, addrDistrict)) return false;
        return true;
    }

    /** Product matchers (shared definition — see ProductMatching): empty = any; set = ANY overlap; categories incl. ancestors. */
    public boolean matchesProduct(Set<java.util.UUID> categoryIds, Set<java.util.UUID> tagIds, Set<java.util.UUID> attributeDefinitionIds) {
        return matchesProduct(new com.viescloud.llc.venzora.service.product.ProductMatching.ProductContext(
                categoryIds == null ? Set.of() : categoryIds, tagIds == null ? Set.of() : tagIds,
                attributeDefinitionIds == null ? Set.of() : attributeDefinitionIds));
    }

    public boolean matchesProduct(com.viescloud.llc.venzora.service.product.ProductMatching.ProductContext ctx) {
        return com.viescloud.llc.venzora.service.product.ProductMatching.matches(tags, categories, attributeDefinitions, ctx);
    }

    private static boolean matchesWithAliases(String matcher, List<String> aliases, String actual) {
        if (eqIgnoreCase(matcher, actual)) return true;
        return aliases != null && aliases.stream().anyMatch(a -> eqIgnoreCase(a, actual));
    }

    private static boolean isSet(String v) {
        return v != null && !v.trim().isEmpty();
    }

    private static boolean eqIgnoreCase(String a, String b) {
        if (a == null || b == null) return false;
        return a.trim().equalsIgnoreCase(b.trim());
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof TaxRule other && id != null && Objects.equals(id, other.id));
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : id.hashCode();
    }
}
