package com.viescloud.llc.venzora.controller.product;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.auto.controller.ViesAutoAdminCheckController;
import com.viescloud.llc.venzora.model.product.ProductVariant;
import com.viescloud.llc.venzora.service.product.ProductVariantService;

@RestController
@RequestMapping("/api/v1/product/variants")
public class ProductVariantController extends ViesAutoAdminCheckController<UUID, ProductVariant, ProductVariantService> {

    public ProductVariantController(ProductVariantService service) {
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
