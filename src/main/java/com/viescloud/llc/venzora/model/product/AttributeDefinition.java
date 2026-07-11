package com.viescloud.llc.venzora.model.product;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.viescloud.eco.viesspringutils.interfaces.annotation.GeneratedUuidV7;
import com.viescloud.llc.venzora.model.product.type.ProductAttributeType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttributeDefinition implements Serializable {

    @Id
    @GeneratedUuidV7
    private UUID id;

    @Column(columnDefinition = "TEXT", unique = true, nullable = false)
    private String name; // e.g., "Size", "Color", "Material"
    
    @Column(columnDefinition = "TEXT")
    private String displayName; // e.g., "Product Size"
    
    @Enumerated(EnumType.STRING)
    private ProductAttributeType type; // TEXT, NUMBER, BOOLEAN, SELECT, MULTI_SELECT
    
    @Column(columnDefinition = "TEXT")
    private String unit; // e.g., "cm", "kg", "%"

    // Predefined options for SELECT/MULTI_SELECT types
    @OneToMany(mappedBy = "attributeDefinition", cascade = {CascadeType.DETACH, CascadeType.REFRESH, CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<AttributeOption> options = new ArrayList<>();

    /**
     * Set the back-reference on every owned child before Hibernate cascades the
     * write. Same rationale as {@link Product#syncChildBackRefs()} — child-side
     * {@code @JsonIgnore} back-references mean incoming JSON never carries them.
     */
    @PrePersist
    @PreUpdate
    private void syncChildBackRefs() {
        if (options != null) {
            for (AttributeOption o : options) {
                if (o != null) o.setAttributeDefinition(this);
            }
        }
    }
}
