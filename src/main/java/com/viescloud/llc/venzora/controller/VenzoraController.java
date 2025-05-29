package com.viescloud.llc.venzora.controller;

import java.io.Serializable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import com.viescloud.eco.viesspringutils.controller.ViesControllerWithCustomUser;
import com.viescloud.eco.viesspringutils.exception.HttpResponseThrowers;
import com.viescloud.eco.viesspringutils.service.ViesService;
import com.viescloud.eco.viesspringutils.util.Strings;
import com.viescloud.llc.venzora.service.authentication.UserService;

public abstract class VenzoraController<I, T extends Serializable, S extends ViesService<I, T, ?>> extends ViesControllerWithCustomUser<I, T, S> {

    @Autowired
    protected UserService userService;
    
    public VenzoraController(S service) {
        super(service);
    }

    @Override
    protected String validateUserId(String user_id, HttpMethod method) {
        super.validateUserId(user_id, method);
        return userService.getByIdOptional(Strings.toLong(user_id).orElse(-1L)).map(e -> user_id)
                          .orElseThrow(HttpResponseThrowers.throwUnauthorizedException("Invalid User Access"));
    }
}
