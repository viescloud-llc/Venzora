package com.viescloud.llc.venzora.service.authentication;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.authentication.OpenIdProviderDao;
import com.viescloud.llc.venzora.model.authentication.OpenIdProvider;
import com.viescloud.llc.venzora.service.VenzoraCustomUserAccessService;

@Service
public class OpenIdProviderService extends VenzoraCustomUserAccessService<Integer, OpenIdProvider, OpenIdProviderDao> {

    public OpenIdProviderService(DatabaseCall<Integer, OpenIdProvider> databaseCall, OpenIdProviderDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public Integer getIdFieldValue(OpenIdProvider object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(OpenIdProvider object, Integer id) {
        object.setId(id);
    }
}
