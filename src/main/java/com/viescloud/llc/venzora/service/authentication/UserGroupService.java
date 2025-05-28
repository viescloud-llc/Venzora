package com.viescloud.llc.venzora.service.authentication;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.eco.viesspringutils.service.ViesService;
import com.viescloud.llc.venzora.dao.authentication.UserGroupDao;
import com.viescloud.llc.venzora.model.authentication.UserGroup;

@Service
public class UserGroupService extends ViesService<Integer, UserGroup, UserGroupDao> {

    public UserGroupService(DatabaseCall<Integer, UserGroup> databaseCall, UserGroupDao dao) {
        super(databaseCall, dao);
    }

    @Override
    protected UserGroup newEmptyObject() {
        return new UserGroup();
    }

    @Override
    public Integer getIdFieldValue(UserGroup object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(UserGroup object, Integer id) {
        object.setId(id);
    }
}
