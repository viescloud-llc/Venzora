package com.viescloud.llc.venzora.dao.authentication;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.viescloud.llc.venzora.model.authentication.UserInfo;

public interface UserInfoDao extends JpaRepository<UserInfo, UUID> {

}
