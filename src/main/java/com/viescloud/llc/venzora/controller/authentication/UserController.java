package com.viescloud.llc.venzora.controller.authentication;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.controller.ViesControllerWithCustomUser;
import com.viescloud.eco.viesspringutils.interfaces.annotation.Permission;
import com.viescloud.eco.viesspringutils.util.Streams;
import com.viescloud.llc.venzora.model.authentication.User;
import com.viescloud.llc.venzora.service.authentication.UserService;

@RestController
@RequestMapping("api/v1/users")
@Permission("ADMIN")
public class UserController extends ViesControllerWithCustomUser<Long, User, UserService> {

    public UserController(UserService service) {
        super(service);
    }

    @GetMapping("public")
    @Permission()
    public List<User> getPublicUser() {
        return Streams.stream(this.service.getAll()).map(e -> {
            return User.builder().id(e.getId()).alias(e.getAlias()).build();
        }).toList();
    }
}
