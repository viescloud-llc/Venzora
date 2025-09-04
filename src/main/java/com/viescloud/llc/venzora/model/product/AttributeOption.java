package com.viescloud.llc.venzora.model.product;

import java.io.Serializable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttributeOption implements Serializable {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.REFRESH, CascadeType.DETACH})
    @JoinColumn(name = "attribute_definition_id")
    private AttributeDefinition attributeDefinition;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String value; // e.g., "Small", "Medium", "Red", "Blue"
    
    @Column(columnDefinition = "TEXT")
    private String displayValue; // e.g., "Small (S)", "Ocean Blue"

    @Column
    private Long sortOrder;
}
