package com.viescloud.llc.venzora.controller.product;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.controller.ViesControllerWithUserAccess;
import com.viescloud.llc.venzora.model.product.WishProduct;
import com.viescloud.llc.venzora.service.product.WishProductService;

@RestController
@RequestMapping("/api/v1/wishlists")
public class WishProductController extends ViesControllerWithUserAccess<UUID, WishProduct, WishProductService> {
    
    public WishProductController(WishProductService service) {
        super(service);
    }
}
