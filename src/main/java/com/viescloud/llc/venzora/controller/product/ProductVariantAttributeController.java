package com.viescloud.llc.venzora.controller.product;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.auto.controller.ViesAutoAdminCheckController;
import com.viescloud.llc.venzora.model.product.ProductVariantAttribute;
import com.viescloud.llc.venzora.service.product.ProductVariantAttributeService;

@RestController
@RequestMapping("/api/v1/product/variant/attributes")
public class ProductVariantAttributeController extends ViesAutoAdminCheckController<UUID, ProductVariantAttribute, ProductVariantAttributeService> {

    public ProductVariantAttributeController(ProductVariantAttributeService service) {
        super(service);
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    /** Authority-based gating (permission-system.md): the seven verbs check catalog:read/create/update/delete. */
    @Override
    protected String resourceName() {
        return "catalog";
    }
    
}
