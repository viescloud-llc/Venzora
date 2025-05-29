package com.viescloud.llc.venzora.service.product;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.CategoryDao;
import com.viescloud.llc.venzora.model.product.Category;
import com.viescloud.llc.venzora.service.VenzoraService;

public class CategoryService extends VenzoraService<Long, Category, CategoryDao> {
    
    public CategoryService(DatabaseCall<Long, Category> databaseCall, CategoryDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public Long getIdFieldValue(Category object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(Category object, Long id) {
        object.setId(id);
    }
}
