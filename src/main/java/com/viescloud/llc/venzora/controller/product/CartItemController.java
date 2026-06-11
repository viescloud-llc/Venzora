package com.viescloud.llc.venzora.controller.product;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.auto.controller.ViesAutoAdminCheckController;
import com.viescloud.llc.venzora.model.product.CartItem;
import com.viescloud.llc.venzora.service.product.CartItemService;

@RestController
@RequestMapping("/api/v1/cart/items")
public class CartItemController extends ViesAutoAdminCheckController<UUID, CartItem, CartItemService> {

    public CartItemController(CartItemService service) {
        super(service);
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

}
