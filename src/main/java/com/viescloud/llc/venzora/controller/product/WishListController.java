package com.viescloud.llc.venzora.controller.product;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.model.MatchByEnum;
import com.viescloud.eco.viesspringutils.model.GenericPropertyMatcherEnum.PropertyMatcherEnum;
import com.viescloud.llc.venzora.controller.VenzoraController;
import com.viescloud.llc.venzora.model.product.WishList;
import com.viescloud.llc.venzora.service.product.WishListService;

@RestController
@RequestMapping("/api/wishlists")
public class WishListController extends VenzoraController<Long, WishList, WishListService> {
    
    public WishListController(WishListService service) {
        super(service);
    }

    @Override
    public ResponseEntity<?> getAll(String arg0, Integer arg1, Integer arg2, WishList arg3, PropertyMatcherEnum arg4,
            MatchByEnum arg5, String arg6) {
        // TODO Auto-generated method stub
        return super.getAll(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
    }
}
