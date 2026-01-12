package com.viescloud.llc.venzora.service.product;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.ProductAttributeDao;
import com.viescloud.llc.venzora.model.product.ProductAttribute;
import com.viescloud.llc.venzora.service.VenzoraService;

@Service
public class ProductAttributeService extends VenzoraService<Long, ProductAttribute, ProductAttributeDao> {

    public ProductAttributeService(DatabaseCall<Long, ProductAttribute> databaseCall,
            ProductAttributeDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public Long getIdFieldValue(ProductAttribute object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(ProductAttribute object, Long id) {
        object.setId(id);
    }
    
}
