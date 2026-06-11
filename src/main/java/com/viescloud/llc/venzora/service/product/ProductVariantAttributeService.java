package com.viescloud.llc.venzora.service.product;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.ProductVariantAttributeDao;
import com.viescloud.llc.venzora.model.product.ProductVariantAttribute;
import com.viescloud.llc.venzora.service.VenzoraService;

@Service
public class ProductVariantAttributeService extends VenzoraService<UUID, ProductVariantAttribute, ProductVariantAttributeDao> {

    public ProductVariantAttributeService(DatabaseCall<UUID, ProductVariantAttribute> databaseCall,
            ProductVariantAttributeDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public UUID getIdFieldValue(ProductVariantAttribute object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(ProductVariantAttribute object, UUID id) {
        object.setId(id);
    }
    
}
