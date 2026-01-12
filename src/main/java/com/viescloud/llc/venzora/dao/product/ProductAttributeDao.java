package com.viescloud.llc.venzora.dao.product;

import org.springframework.data.jpa.repository.JpaRepository;

import com.viescloud.llc.venzora.model.product.ProductAttribute;

public interface ProductAttributeDao extends JpaRepository<ProductAttribute, Long> {
    
}
