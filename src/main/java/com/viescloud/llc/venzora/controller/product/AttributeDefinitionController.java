package com.viescloud.llc.venzora.controller.product;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.auto.controller.ViesAutoAdminCheckController;
import com.viescloud.llc.venzora.model.product.AttributeDefinition;
import com.viescloud.llc.venzora.service.product.AttributeDefinitionService;

@RestController
@RequestMapping("/api/v1/product/attribute/definitions")
public class AttributeDefinitionController extends ViesAutoAdminCheckController<UUID, AttributeDefinition, AttributeDefinitionService> {

    public AttributeDefinitionController(AttributeDefinitionService service) {
        super(service);
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    /** Authority-based gating (permission-system.md): the seven verbs check schema:read/create/update/delete. */
    @Override
    protected String resourceName() {
        return "schema";
    }
    
}
