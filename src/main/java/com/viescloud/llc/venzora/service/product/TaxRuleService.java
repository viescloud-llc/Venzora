package com.viescloud.llc.venzora.service.product;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.TaxRuleDao;
import com.viescloud.llc.venzora.model.product.TaxRule;
import com.viescloud.llc.venzora.service.VenzoraService;

@Service
public class TaxRuleService extends VenzoraService<UUID, TaxRule, TaxRuleDao> {

    public TaxRuleService(DatabaseCall<UUID, TaxRule> databaseCall, TaxRuleDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public UUID getIdFieldValue(TaxRule object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(TaxRule object, UUID id) {
        object.setId(id);
    }

}
