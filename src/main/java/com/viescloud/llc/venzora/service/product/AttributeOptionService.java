package com.viescloud.llc.venzora.service.product;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.AttributeOptionDao;
import com.viescloud.llc.venzora.model.product.AttributeOption;
import com.viescloud.llc.venzora.service.VenzoraService;

@Service
public class AttributeOptionService extends VenzoraService<Long, AttributeOption, AttributeOptionDao> {

    public AttributeOptionService(DatabaseCall<Long, AttributeOption> databaseCall, AttributeOptionDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public Long getIdFieldValue(AttributeOption object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(AttributeOption object, Long id) {
        object.setId(id);
    }
    
}
