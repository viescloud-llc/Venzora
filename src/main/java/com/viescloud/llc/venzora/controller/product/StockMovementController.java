package com.viescloud.llc.venzora.controller.product;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.auto.controller.ViesAutoAdminCheckController;
import com.viescloud.llc.venzora.model.product.StockMovement;
import com.viescloud.llc.venzora.service.product.StockMovementService;

@RestController
@RequestMapping("/api/v1/stock/movements")
public class StockMovementController extends ViesAutoAdminCheckController<UUID, StockMovement, StockMovementService> {

    public StockMovementController(StockMovementService service) {
        super(service);
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    /** Authority-based gating (permission-system.md): the seven verbs check inventory:read/create/update/delete. */
    @Override
    protected String resourceName() {
        return "inventory";
    }

}
