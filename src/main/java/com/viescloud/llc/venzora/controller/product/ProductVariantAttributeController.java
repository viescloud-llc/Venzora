package com.viescloud.llc.venzora.controller.product;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.auto.controller.ViesAutoAdminCheckController;
import com.viescloud.llc.venzora.model.product.ProductVariantAttribute;
import com.viescloud.llc.venzora.service.product.ProductVariantAttributeService;

@RestController
@RequestMapping("/api/v1/product/variant/attributes")
public class ProductVariantAttributeController extends ViesAutoAdminCheckController<Long, ProductVariantAttribute, ProductVariantAttributeService> {

    public ProductVariantAttributeController(ProductVariantAttributeService service) {
        super(service);
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }
    
}
