package com.viescloud.llc.venzora.service.product;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.ProductVariantDao;
import com.viescloud.llc.venzora.model.product.ProductVariant;
import com.viescloud.llc.venzora.service.VenzoraService;

@Service
public class ProductVariantService extends VenzoraService<UUID, ProductVariant, ProductVariantDao> {

    public ProductVariantService(DatabaseCall<UUID, ProductVariant> databaseCall, ProductVariantDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public UUID getIdFieldValue(ProductVariant object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(ProductVariant object, UUID id) {
        object.setId(id);
    }
    
}
