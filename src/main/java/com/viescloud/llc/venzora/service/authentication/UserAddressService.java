package com.viescloud.llc.venzora.service.authentication;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.authentication.UserAddressDao;
import com.viescloud.llc.venzora.model.authentication.UserAddress;
import com.viescloud.llc.venzora.service.VenzoraService;

public class UserAddressService extends VenzoraService<Long, UserAddress, UserAddressDao> {

    public UserAddressService(DatabaseCall<Long, UserAddress> databaseCall, UserAddressDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public Long getIdFieldValue(UserAddress object) {
        return object.getUserId();
    }

    @Override
    public void setIdFieldValue(UserAddress object, Long id) {
        object.setUserId(id);
    }
    
}
