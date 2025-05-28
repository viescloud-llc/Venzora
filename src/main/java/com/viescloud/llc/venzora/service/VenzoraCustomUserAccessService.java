package com.viescloud.llc.venzora.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.viescloud.eco.viesspringutils.dao.ViesUserAccessJpaRepository;
import com.viescloud.eco.viesspringutils.model.UserAccess;
import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.eco.viesspringutils.service.ViesServiceWithCustomUserAccess;
import com.viescloud.llc.venzora.model.authentication.User;
import com.viescloud.llc.venzora.service.authentication.UserService;

public abstract class VenzoraCustomUserAccessService<I, T extends UserAccess, D extends ViesUserAccessJpaRepository<T, I>> extends ViesServiceWithCustomUserAccess<I, T, D, User> {
    
    @Autowired
    protected UserService userService;

    public VenzoraCustomUserAccessService(DatabaseCall<I, T> databaseCall, D repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    protected List<String> getAllUserGroups(String userId) {
        var user = this.getUser(userId);
        return user.getUserGroups().stream().map(UserGroup -> UserGroup.getId() + "").toList();
    }

    @Override
    protected User getUser(String userId) {
        return this.userService.getById(Long.parseLong(userId));
    }
}
