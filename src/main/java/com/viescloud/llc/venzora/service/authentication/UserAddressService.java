package com.viescloud.llc.venzora.service.authentication;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.authentication.UserAddressDao;
import com.viescloud.llc.venzora.model.authentication.UserAddress;
import com.viescloud.llc.venzora.service.VenzoraService;

@Service
public class UserAddressService extends VenzoraService<UUID, UserAddress, UserAddressDao> {

    public UserAddressService(DatabaseCall<UUID, UserAddress> databaseCall, UserAddressDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public UUID getIdFieldValue(UserAddress object) {
        return object.getUserId();
    }

    @Override
    public void setIdFieldValue(UserAddress object, UUID id) {
        object.setUserId(id);
    }

}
