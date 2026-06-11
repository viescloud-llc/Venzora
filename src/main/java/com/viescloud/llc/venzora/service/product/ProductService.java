package com.viescloud.llc.venzora.service.product;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.ProductDao;
import com.viescloud.llc.venzora.model.product.Product;
import com.viescloud.llc.venzora.service.VenzoraService;

@Service
public class ProductService extends VenzoraService<UUID, Product, ProductDao> {

    public ProductService(DatabaseCall<UUID, Product> databaseCall, ProductDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public UUID getIdFieldValue(Product object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(Product object, UUID id) {
        object.setId(id);
    }
    
}
