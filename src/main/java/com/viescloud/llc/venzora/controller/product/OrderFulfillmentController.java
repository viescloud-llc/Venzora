package com.viescloud.llc.venzora.controller.product;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.controller.ViesControllerWithUserAccess;
import com.viescloud.llc.venzora.model.product.OrderFulfillment;
import com.viescloud.llc.venzora.service.product.OrderFulfillmentService;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderFulfillmentController extends ViesControllerWithUserAccess<UUID, OrderFulfillment, OrderFulfillmentService> {

    public OrderFulfillmentController(OrderFulfillmentService service) {
        super(service);
    }

}
