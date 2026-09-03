package com.viescloud.llc.venzora.controller.product;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.auto.controller.ViesAutoAdminCheckController;
import com.viescloud.llc.venzora.model.product.Discount;
import com.viescloud.llc.venzora.service.product.DiscountService;

@RestController
@RequestMapping("/api/v1/discounts")
public class DiscountController extends ViesAutoAdminCheckController<UUID, Discount, DiscountService> {

    public DiscountController(DiscountService service) {
        super(service);
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    /** Authority-based gating (permission-system.md): the seven verbs check discounts:read/create/update/delete. */
    @Override
    protected String resourceName() {
        return "discounts";
    }

}
