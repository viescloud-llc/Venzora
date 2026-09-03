package com.viescloud.llc.venzora.controller.product;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.auto.controller.ViesAutoAdminCheckController;
import com.viescloud.llc.venzora.model.product.ProductMedia;
import com.viescloud.llc.venzora.service.product.ProductMediaService;

@RestController
@RequestMapping("/api/v1/product/medias")
public class ProductMediaController extends ViesAutoAdminCheckController<UUID, ProductMedia, ProductMediaService> {

    public ProductMediaController(ProductMediaService service) {
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
