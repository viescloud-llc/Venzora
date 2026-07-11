package com.viescloud.llc.venzora.dao.product;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.viescloud.llc.venzora.model.product.ShippingRule;
import com.viescloud.llc.venzora.model.share_enum.Currency;

public interface ShippingRuleDao extends JpaRepository<ShippingRule, UUID> {

    Optional<ShippingRule> findByCurrency(Currency currency);
}
