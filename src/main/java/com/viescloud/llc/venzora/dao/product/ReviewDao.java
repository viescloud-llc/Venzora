package com.viescloud.llc.venzora.dao.product;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.viescloud.llc.venzora.model.product.Review;

public interface ReviewDao extends JpaRepository<Review, UUID> {

    Page<Review> findAllByUserId(UUID userId, Pageable pageable);

    Page<Review> findAllByProductId(UUID productId, Pageable pageable);
}
