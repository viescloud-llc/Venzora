package com.viescloud.llc.venzora.service.product;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.ProductAttributeDao;
import com.viescloud.llc.venzora.model.product.ProductAttribute;
import com.viescloud.llc.venzora.service.VenzoraService;

@Service
public class ProductAttributeService extends VenzoraService<UUID, ProductAttribute, ProductAttributeDao> {

    public ProductAttributeService(DatabaseCall<UUID, ProductAttribute> databaseCall,
            ProductAttributeDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public UUID getIdFieldValue(ProductAttribute object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(ProductAttribute object, UUID id) {
        object.setId(id);
    }
    
}
