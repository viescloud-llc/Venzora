package com.viescloud.llc.venzora.controller.product;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.auto.controller.ViesAutoAdminCheckController;
import com.viescloud.llc.venzora.model.product.OrderFulfillmentItem;
import com.viescloud.llc.venzora.service.product.OrderFulfillmentItemService;

@RestController
@RequestMapping("/api/v1/order/items")
public class OrderFulfillmentItemController extends ViesAutoAdminCheckController<UUID, OrderFulfillmentItem, OrderFulfillmentItemService> {

    public OrderFulfillmentItemController(OrderFulfillmentItemService service) {
        super(service);
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    /** Authority-based gating (permission-system.md): the seven verbs check orders:read/create/update/delete. */
    @Override
    protected String resourceName() {
        return "orders";
    }

}
