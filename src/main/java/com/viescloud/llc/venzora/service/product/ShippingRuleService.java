package com.viescloud.llc.venzora.service.product;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.ShippingRuleDao;
import com.viescloud.llc.venzora.model.product.ShippingRule;
import com.viescloud.llc.venzora.service.VenzoraService;

@Service
public class ShippingRuleService extends VenzoraService<UUID, ShippingRule, ShippingRuleDao> {

    public ShippingRuleService(DatabaseCall<UUID, ShippingRule> databaseCall, ShippingRuleDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public UUID getIdFieldValue(ShippingRule object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(ShippingRule object, UUID id) {
        object.setId(id);
    }

}
