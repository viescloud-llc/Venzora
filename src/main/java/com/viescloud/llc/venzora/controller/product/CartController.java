package com.viescloud.llc.venzora.controller.product;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.controller.ViesControllerWithUserAccess;
import com.viescloud.llc.venzora.model.product.Cart;
import com.viescloud.llc.venzora.service.product.CartService;

@RestController
@RequestMapping("/api/v1/carts")
public class CartController extends ViesControllerWithUserAccess<UUID, Cart, CartService> {

    public CartController(CartService service) {
        super(service);
    }

}
