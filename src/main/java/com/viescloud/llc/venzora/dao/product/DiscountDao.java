package com.viescloud.llc.venzora.dao.product;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.viescloud.llc.venzora.model.product.Discount;

public interface DiscountDao extends JpaRepository<Discount, UUID> {

    Optional<Discount> findByCode(String code);
}
