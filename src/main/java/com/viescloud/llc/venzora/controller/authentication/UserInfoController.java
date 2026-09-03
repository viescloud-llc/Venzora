package com.viescloud.llc.venzora.controller.authentication;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.auto.controller.ViesAutoAdminCheckController;
import com.viescloud.llc.venzora.model.authentication.UserInfo;
import com.viescloud.llc.venzora.service.authentication.UserInfoService;

@RestController
@RequestMapping("/api/v1/user/infos")
public class UserInfoController extends ViesAutoAdminCheckController<UUID, UserInfo, UserInfoService> {

    public UserInfoController(UserInfoService service) {
        super(service);
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    /** Authority-based gating (permission-system.md): the seven verbs check customers:read/create/update/delete. */
    @Override
    protected String resourceName() {
        return "customers";
    }

}
