package com.viescloud.llc.venzora.model.product;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.viescloud.eco.viesspringutils.interfaces.annotation.GeneratedUuidV7;
import com.viescloud.llc.venzora.model.product.type.ProductMediaType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class ProductMedia implements Serializable {

    @Id
    @GeneratedUuidV7
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.REFRESH, CascadeType.DETACH})
    @JoinColumn(name = "product_id")
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.REFRESH, CascadeType.DETACH})
    @JoinColumn(name = "product_variant_id")
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private ProductVariant productVariant;

    /**
     * Direct URL to the media asset. Optional — either this or
     * {@link #objectStorageDataId} must be set (see {@link #validateSource()}).
     */
    @Column(columnDefinition = "TEXT")
    private String url;

    /**
     * UUID of a data object in the object storage service. Optional — either
     * this or {@link #url} must be set (see {@link #validateSource()}).
     */
    @Column
    private UUID objectStorageDataId;

    @Enumerated(EnumType.STRING)
    private ProductMediaType mediaType; // IMAGE, VIDEO, etc.

    @Column(columnDefinition = "TEXT")
    private String altText;

    @Column(columnDefinition = "TEXT")
    private String caption;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Boolean isPrimary = false;

    /**
     * Enforces that at least one of {@code url} / {@code objectStorageDataId}
     * is set on every insert or update. Fires at the JPA lifecycle boundary so
     * the same rule applies to auto-CRUD writes, orchestrator writes, and
     * cascaded saves from the parent Product / ProductVariant.
     */
    @PrePersist
    @PreUpdate
    private void validateSource() {
        boolean hasUrl = url != null && !url.isBlank();
        boolean hasStorageRef = objectStorageDataId != null;
        if (!hasUrl && !hasStorageRef) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ProductMedia requires either 'url' or 'objectStorageDataId' to be set");
        }
    }

    /**
     * Normalize a media collection so exactly one item is marked primary.
     *
     * <ul>
     *   <li>Empty collection → no change.</li>
     *   <li>Single item → forced primary (the "default" case).</li>
     *   <li>Multiple items → deterministic winner: prefer an already-primary item;
     *       break ties by lowest {@code sortOrder}, then smallest {@code id}
     *       (UUIDv7 is time-ordered, so the oldest wins). The rest are set
     *       non-primary.</li>
     * </ul>
     *
     * Called from {@code Product.syncChildBackRefs()} and
     * {@code ProductVariant.syncChildBackRefs()} so the rule is enforced on
     * every write path — auto-CRUD, orchestrator, and cascaded parent saves.
     */
    public static void normalizePrimary(Collection<ProductMedia> medias) {
        if (medias == null || medias.isEmpty()) return;

        List<ProductMedia> items = new ArrayList<>(medias.size());
        for (ProductMedia m : medias) {
            if (m != null) items.add(m);
        }
        if (items.isEmpty()) return;

        if (items.size() == 1) {
            items.get(0).setIsPrimary(Boolean.TRUE);
            return;
        }

        Comparator<ProductMedia> tiebreaker = Comparator
                .comparing(ProductMedia::getSortOrder,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ProductMedia::getId,
                        Comparator.nullsLast(Comparator.naturalOrder()));

        // Prefer a media that's already flagged primary; if none, pick the best by tiebreaker.
        ProductMedia winner = items.stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsPrimary()))
                .min(tiebreaker)
                .orElseGet(() -> items.stream().min(tiebreaker).orElse(null));

        if (winner == null) return;

        for (ProductMedia m : items) {
            m.setIsPrimary(m == winner);
        }
    }
}
