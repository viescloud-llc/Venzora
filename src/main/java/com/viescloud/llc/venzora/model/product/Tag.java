package com.viescloud.llc.venzora.model.product;

import java.io.Serializable;
import java.util.UUID;

import com.viescloud.eco.viesspringutils.interfaces.annotation.GeneratedUuidV7;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Tag implements Serializable {
    
    @Id
    @GeneratedUuidV7
    private UUID id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;
}
