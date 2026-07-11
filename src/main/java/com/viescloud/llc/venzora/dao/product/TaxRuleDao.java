package com.viescloud.llc.venzora.dao.product;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.viescloud.llc.venzora.model.product.TaxRule;

public interface TaxRuleDao extends JpaRepository<TaxRule, UUID> {

    List<TaxRule> findAllByActiveTrue();
}
