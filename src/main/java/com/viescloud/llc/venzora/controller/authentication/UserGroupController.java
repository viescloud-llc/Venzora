package com.viescloud.llc.venzora.controller.authentication;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.controller.ViesControllerWithCustomUser;
import com.viescloud.llc.venzora.model.authentication.UserGroup;
import com.viescloud.llc.venzora.service.authentication.UserGroupService;

@RestController
@RequestMapping("/api/v1/user/groups")
public class UserGroupController extends ViesControllerWithCustomUser<Integer, UserGroup, UserGroupService> {

    public UserGroupController(UserGroupService service) {
        super(service);
    }
}
