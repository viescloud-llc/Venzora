package com.viescloud.llc.venzora.controller.product;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.auto.controller.ViesAutoAdminCheckController;
import com.viescloud.llc.venzora.model.product.OrderItem;
import com.viescloud.llc.venzora.service.product.OrderItemService;

@RestController
@RequestMapping("/api/v1/order/items")
public class OrderItemController extends ViesAutoAdminCheckController<UUID, OrderItem, OrderItemService> {

    public OrderItemController(OrderItemService service) {
        super(service);
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

}
