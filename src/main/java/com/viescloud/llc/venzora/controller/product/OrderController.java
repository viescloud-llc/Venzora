package com.viescloud.llc.venzora.controller.product;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.controller.ViesControllerWithUserAccess;
import com.viescloud.llc.venzora.model.product.Order;
import com.viescloud.llc.venzora.service.product.OrderService;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController extends ViesControllerWithUserAccess<UUID, Order, OrderService> {

    public OrderController(OrderService service) {
        super(service);
    }

}
