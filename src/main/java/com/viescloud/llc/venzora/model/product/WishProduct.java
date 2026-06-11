package com.viescloud.llc.venzora.model.product;

import java.util.UUID;

import com.viescloud.eco.viesspringutils.interfaces.annotation.GeneratedUuidV7;
import com.viescloud.eco.viesspringutils.model.TrackedTimeStampUserAccess;

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
public class WishProduct extends TrackedTimeStampUserAccess {
    
    @Id
    @GeneratedUuidV7
    private UUID id;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false) 
    private Long quantity;
}
