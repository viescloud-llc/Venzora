package com.viescloud.llc.venzora.service.product;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.ProductVariantDao;
import com.viescloud.llc.venzora.model.product.ProductVariant;
import com.viescloud.llc.venzora.service.VenzoraService;

@Service
public class ProductVariantService extends VenzoraService<Long, ProductVariant, ProductVariantDao> {

    public ProductVariantService(DatabaseCall<Long, ProductVariant> databaseCall, ProductVariantDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public Long getIdFieldValue(ProductVariant object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(ProductVariant object, Long id) {
        object.setId(id);
    }
    
}
