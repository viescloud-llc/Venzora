package com.viescloud.llc.venzora.controller.authentication;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.auto.controller.ViesAutoAdminCheckController;
import com.viescloud.llc.venzora.model.authentication.UserAddress;
import com.viescloud.llc.venzora.service.authentication.UserAddressService;

@RestController
@RequestMapping("/api/v1/user/addresses")
public class UserAddressController extends ViesAutoAdminCheckController<UUID, UserAddress, UserAddressService> {

    public UserAddressController(UserAddressService service) {
        super(service);
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

}
