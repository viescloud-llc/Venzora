package com.viescloud.llc.venzora.service.authentication;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.viescloud.eco.viesspringutils.model.EncodingType;
import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.eco.viesspringutils.service.ViesService;
import com.viescloud.eco.viesspringutils.util.CollectionUtils;
import com.viescloud.eco.viesspringutils.util.DataTransformationUtils;
import com.viescloud.eco.viesspringutils.util.Streams;
import com.viescloud.llc.venzora.dao.authentication.UserDao;
import com.viescloud.llc.venzora.dao.authentication.UserGroupDao;
import com.viescloud.llc.venzora.model.authentication.User;
import com.viescloud.llc.venzora.model.authentication.UserGroup;

import jakarta.annotation.PostConstruct;

@Service
public class UserService extends ViesService<Long, User, UserDao> {

    @Autowired
    private UserGroupDao userGroupDao;

    public UserService(DatabaseCall<Long, User> databaseCall, UserDao dao) {
        super(databaseCall, dao);
    }

    @Override
    protected User newEmptyObject() {
        return new User();
    }

    @Override
    public Long getIdFieldValue(User object) {
        return object.getId();
    }

    @PostConstruct
    public void init() {
        var list = this.repositoryDao.findAll();
        if(ObjectUtils.isEmpty(list)) {
            var adminGroup = CollectionUtils.getExactValue(userGroupDao.findAllByName("ADMIN"), g -> g.getName().equals("ADMIN"));
            if(ObjectUtils.isEmpty(adminGroup)) {
                adminGroup = new UserGroup();
                adminGroup.setName("ADMIN");
                adminGroup.setDescription("Auto created ADMIN group");
                adminGroup = this.userGroupDao.save(adminGroup);
            }

            var user = new User();
            user.setUsername("admin");
            user.setEmail("admin");
            user.setAlias("admin");
            user.setPassword(DataTransformationUtils.encode("admin", EncodingType.SHA256));
            user.setUserGroups(Set.of(adminGroup));
            this.databaseCall.save(user);
        }
    }

    public Optional<User> getByUsername(String username) {
        return Streams.stream(this.repositoryDao.findAllByUsername(username)).filter(e -> e.getUsername().equals(username)).findFirst();
    }

    public Optional<User> getByEmail(String email) {
        return Streams.stream(this.repositoryDao.findAllByEmail(email)).filter(e -> e.getEmail().equals(email)).findFirst();
    }

    public boolean isUsernameExist(String username) {
        return this.getByUsername(username).isPresent();
    }   

    public boolean isEmailExist(String email) {
        return this.getByEmail(email).isPresent();
    }

    public List<String> getAllUserGroups(String userId) {
        var user = this.getById(Long.parseLong(userId));
        return user.getUserGroups().stream().map(UserGroup -> UserGroup.getName()).toList();
    }

    public Optional<User> getBySub(String sub) {
        return this.repositoryDao.findBySub(sub);
    }

    @Override
    public void setIdFieldValue(User object, Long id) {
        object.setId(id);
    }
}
