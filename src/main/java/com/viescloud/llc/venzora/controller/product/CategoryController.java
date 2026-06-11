package com.viescloud.llc.venzora.controller.product;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.auto.controller.ViesAutoAdminCheckController;
import com.viescloud.llc.venzora.model.product.Category;
import com.viescloud.llc.venzora.service.product.CategoryService;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController extends ViesAutoAdminCheckController<UUID, Category, CategoryService> {

    public CategoryController(CategoryService service) {
        super(service);
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

}
