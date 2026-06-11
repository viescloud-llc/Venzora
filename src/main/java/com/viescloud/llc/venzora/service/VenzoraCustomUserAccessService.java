package com.viescloud.llc.venzora.service;

import com.viescloud.eco.viesspringutils.auto.service.ViesAutoServiceWithUserAccess;
import com.viescloud.eco.viesspringutils.dao.ViesUserAccessJpaRepository;
import com.viescloud.eco.viesspringutils.model.UserAccess;
import com.viescloud.eco.viesspringutils.repository.DatabaseCall;

public abstract class VenzoraCustomUserAccessService<I, T extends UserAccess, D extends ViesUserAccessJpaRepository<T, I>> extends ViesAutoServiceWithUserAccess<I, T, D> {

    public VenzoraCustomUserAccessService(DatabaseCall<I, T> databaseCall, D repositoryDao) {
        super(databaseCall, repositoryDao);
    }

}
