package com.viescloud.llc.venzora.dao.product;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.viescloud.llc.venzora.model.product.ProductVariant;

public interface ProductVariantDao extends JpaRepository<ProductVariant, UUID> {

    /** SKU is globally unique across ALL variants — used by the generator to skip collisions. */
    boolean existsBySku(String sku);
}
