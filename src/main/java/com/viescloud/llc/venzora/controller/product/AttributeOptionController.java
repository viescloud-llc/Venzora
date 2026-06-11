package com.viescloud.llc.venzora.controller.product;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.auto.controller.ViesAutoAdminCheckController;
import com.viescloud.llc.venzora.model.product.AttributeOption;
import com.viescloud.llc.venzora.service.product.AttributeOptionService;

@RestController
@RequestMapping("/api/v1/product/attribute/options")
public class AttributeOptionController extends ViesAutoAdminCheckController<UUID, AttributeOption, AttributeOptionService> {

    public AttributeOptionController(AttributeOptionService service) {
        super(service);
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }
    
}
