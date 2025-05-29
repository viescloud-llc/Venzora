package com.viescloud.llc.venzora.model.product;

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
public class WishList extends TrackedTimeStamp {
    
    @Id
    private Long userId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false) 
    private Long quantity;
}
