package com.viescloud.llc.venzora.dao.product;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.viescloud.llc.venzora.model.product.ProductMedia;

public interface ProductMediaDao extends JpaRepository<ProductMedia, UUID> {

}
