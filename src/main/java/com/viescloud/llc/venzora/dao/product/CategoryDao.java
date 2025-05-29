package com.viescloud.llc.venzora.dao.product;


import org.springframework.data.jpa.repository.JpaRepository;

import com.viescloud.llc.venzora.model.product.Category;

public interface CategoryDao extends JpaRepository<Category, Long> {
    
}
