package com.viescloud.llc.venzora.service.authentication;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.authentication.UserInfoDao;
import com.viescloud.llc.venzora.model.authentication.UserInfo;
import com.viescloud.llc.venzora.service.VenzoraService;

public class UserInfoService extends VenzoraService<Long, UserInfo, UserInfoDao> {

    public UserInfoService(DatabaseCall<Long, UserInfo> databaseCall, UserInfoDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public Long getIdFieldValue(UserInfo object) {
        return object.getUserId();
    }

    @Override
    public void setIdFieldValue(UserInfo object, Long id) {
        object.setUserId(id);
    }
    
}
