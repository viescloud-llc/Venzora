package com.viescloud.llc.venzora.controller.product;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.auto.controller.ViesAutoAdminCheckController;
import com.viescloud.llc.venzora.model.product.AttributeDefinition;
import com.viescloud.llc.venzora.service.product.AttributeDefinitionService;

@RestController
@RequestMapping("/api/v1/product/attribute/definitions")
public class AttributeDefinitionController extends ViesAutoAdminCheckController<Long, AttributeDefinition, AttributeDefinitionService> {

    public AttributeDefinitionController(AttributeDefinitionService service) {
        super(service);
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }
    
}
