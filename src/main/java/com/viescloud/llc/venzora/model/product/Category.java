package com.viescloud.llc.venzora.model.product;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.viescloud.eco.viesspringutils.interfaces.annotation.GeneratedUuidV7;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Transient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Category implements Serializable {
    
    @Id
    @GeneratedUuidV7
    private UUID id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private UUID parentCategoryId;

    @ManyToMany
    @JoinTable(
        name = "category_attribute_definitions",
        joinColumns = @JoinColumn(name = "category_id"),
        inverseJoinColumns = @JoinColumn(name = "attribute_definition_id")
    )
    private List<AttributeDefinition> attributeDefinitions = new ArrayList<>();

    @Transient
    private Category parentCategory;

    @Transient
    private Set<Category> childrenCategories;
}
