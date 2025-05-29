package com.viescloud.llc.venzora.dao.product;

import org.springframework.data.jpa.repository.JpaRepository;

import com.viescloud.llc.venzora.model.product.Review;

public interface ReviewDao extends JpaRepository<Review, Long> {
    
}
