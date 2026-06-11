package com.viescloud.llc.venzora.controller.product;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.auto.controller.ViesAutoAdminCheckController;
import com.viescloud.llc.venzora.model.product.Shipment;
import com.viescloud.llc.venzora.service.product.ShipmentService;

@RestController
@RequestMapping("/api/v1/shipments")
public class ShipmentController extends ViesAutoAdminCheckController<UUID, Shipment, ShipmentService> {

    public ShipmentController(ShipmentService service) {
        super(service);
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

}
