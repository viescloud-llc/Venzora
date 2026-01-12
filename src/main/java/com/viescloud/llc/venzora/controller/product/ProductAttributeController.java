package com.viescloud.llc.venzora.controller.product;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.auto.controller.ViesAutoAdminCheckController;
import com.viescloud.llc.venzora.model.product.ProductAttribute;
import com.viescloud.llc.venzora.service.product.ProductAttributeService;

@RestController
@RequestMapping("/api/v1/product/attributes")
public class ProductAttributeController extends ViesAutoAdminCheckController<Long, ProductAttribute, ProductAttributeService> {

    public ProductAttributeController(ProductAttributeService service) {
        super(service);
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }
    
}
