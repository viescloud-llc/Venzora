package com.viescloud.llc.venzora.service.product;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.AttributeDefinitionDao;
import com.viescloud.llc.venzora.model.product.AttributeDefinition;
import com.viescloud.llc.venzora.service.VenzoraService;

@Service
public class AttributeDefinitionService extends VenzoraService<Long, AttributeDefinition, AttributeDefinitionDao> {

    public AttributeDefinitionService(DatabaseCall<Long, AttributeDefinition> databaseCall,
            AttributeDefinitionDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public Long getIdFieldValue(AttributeDefinition object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(AttributeDefinition object, Long id) {
        object.setId(id);
    }
    
}
