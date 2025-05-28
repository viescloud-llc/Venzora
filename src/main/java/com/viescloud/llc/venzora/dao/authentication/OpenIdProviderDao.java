package com.viescloud.llc.venzora.dao.authentication;

import org.springframework.stereotype.Repository;

import com.viescloud.eco.viesspringutils.dao.ViesUserAccessJpaRepository;
import com.viescloud.llc.venzora.model.authentication.OpenIdProvider;

@Repository
public interface OpenIdProviderDao extends ViesUserAccessJpaRepository<OpenIdProvider, Integer> {
    
}