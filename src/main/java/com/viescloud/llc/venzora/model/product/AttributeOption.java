package com.viescloud.llc.venzora.model.product;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.viescloud.eco.viesspringutils.interfaces.annotation.GeneratedUuidV7;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttributeOption implements Serializable {

    @Id
    @GeneratedUuidV7
    private UUID id;

    @Column(name = "attribute_option_value", columnDefinition = "TEXT", nullable = false)
    private String value; // e.g., "Small", "Medium", "Red", "Blue"
    
    @Column(columnDefinition = "TEXT")
    private String displayValue; // e.g., "Small (S)", "Ocean Blue"

    @Column
    private Long sortOrder;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.REFRESH, CascadeType.DETACH})
    @JoinColumn(name = "attribute_definition_id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private AttributeDefinition attributeDefinition;
}
