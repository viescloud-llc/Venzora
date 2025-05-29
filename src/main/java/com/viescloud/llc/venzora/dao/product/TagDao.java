package com.viescloud.llc.venzora.dao.product;

import org.springframework.data.jpa.repository.JpaRepository;

import com.viescloud.llc.venzora.model.product.Tag;

public interface TagDao extends JpaRepository<Tag, Long> {
    
}
