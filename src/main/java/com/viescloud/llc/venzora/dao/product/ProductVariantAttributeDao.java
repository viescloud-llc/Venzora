package com.viescloud.llc.venzora.dao.product;

import org.springframework.data.jpa.repository.JpaRepository;

import com.viescloud.llc.venzora.model.product.ProductVariantAttribute;

public interface ProductVariantAttributeDao extends JpaRepository<ProductVariantAttribute, Long> {
    
}
