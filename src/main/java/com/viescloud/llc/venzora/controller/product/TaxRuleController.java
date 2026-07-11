package com.viescloud.llc.venzora.controller.product;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.auto.controller.ViesAutoAdminCheckController;
import com.viescloud.llc.venzora.model.product.TaxRule;
import com.viescloud.llc.venzora.service.product.TaxRuleService;

@RestController
@RequestMapping("/api/v1/tax/rules")
public class TaxRuleController extends ViesAutoAdminCheckController<UUID, TaxRule, TaxRuleService> {

    public TaxRuleController(TaxRuleService service) {
        super(service);
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

}
