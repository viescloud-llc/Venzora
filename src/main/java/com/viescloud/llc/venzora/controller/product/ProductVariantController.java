package com.viescloud.llc.venzora.controller.product;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.auto.controller.ViesAutoAdminCheckController;
import com.viescloud.llc.venzora.model.product.ProductVariant;
import com.viescloud.llc.venzora.service.product.ProductVariantService;

@RestController
@RequestMapping("/api/v1/product/variants")
public class ProductVariantController extends ViesAutoAdminCheckController<Long, ProductVariant, ProductVariantService> {

    public ProductVariantController(ProductVariantService service) {
        super(service);
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }
    
}
