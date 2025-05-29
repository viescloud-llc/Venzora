package com.viescloud.llc.venzora.service.product;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.ProductDao;
import com.viescloud.llc.venzora.model.product.Product;
import com.viescloud.llc.venzora.service.VenzoraService;

public class ProductService extends VenzoraService<Long, Product, ProductDao> {

    public ProductService(DatabaseCall<Long, Product> databaseCall, ProductDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public Long getIdFieldValue(Product object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(Product object, Long id) {
        object.setId(id);
    }
    
}
