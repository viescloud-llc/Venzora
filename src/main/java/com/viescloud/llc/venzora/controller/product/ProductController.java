package com.viescloud.llc.venzora.controller.product;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.auto.controller.ViesAutoAdminCheckController;
import com.viescloud.llc.venzora.model.product.Product;
import com.viescloud.llc.venzora.service.product.ProductService;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController extends ViesAutoAdminCheckController<UUID, Product, ProductService> {

    public ProductController(ProductService service) {
        super(service);
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }
    
}
