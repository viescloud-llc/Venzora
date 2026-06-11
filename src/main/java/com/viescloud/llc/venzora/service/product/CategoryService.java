package com.viescloud.llc.venzora.service.product;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.CategoryDao;
import com.viescloud.llc.venzora.model.product.Category;
import com.viescloud.llc.venzora.service.VenzoraService;

@Service
public class CategoryService extends VenzoraService<UUID, Category, CategoryDao> {
    
    public CategoryService(DatabaseCall<UUID, Category> databaseCall, CategoryDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public UUID getIdFieldValue(Category object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(Category object, UUID id) {
        object.setId(id);
    }
}
