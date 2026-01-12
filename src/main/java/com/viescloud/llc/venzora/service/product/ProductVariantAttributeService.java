package com.viescloud.llc.venzora.service.product;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.ProductVariantAttributeDao;
import com.viescloud.llc.venzora.model.product.ProductVariantAttribute;
import com.viescloud.llc.venzora.service.VenzoraService;

@Service
public class ProductVariantAttributeService extends VenzoraService<Long, ProductVariantAttribute, ProductVariantAttributeDao> {

    public ProductVariantAttributeService(DatabaseCall<Long, ProductVariantAttribute> databaseCall,
            ProductVariantAttributeDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public Long getIdFieldValue(ProductVariantAttribute object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(ProductVariantAttribute object, Long id) {
        object.setId(id);
    }
    
}
