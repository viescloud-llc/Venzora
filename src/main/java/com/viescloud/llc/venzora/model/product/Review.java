package com.viescloud.llc.venzora.model.product;

import java.math.BigDecimal;
import java.util.UUID;

import com.viescloud.eco.viesspringutils.interfaces.annotation.GeneratedUuidV7;
import com.viescloud.eco.viesspringutils.model.TrackedTimeStamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Review extends TrackedTimeStamp {
    
    @Id
    @GeneratedUuidV7
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID productId;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false)
    private BigDecimal rating;
}
