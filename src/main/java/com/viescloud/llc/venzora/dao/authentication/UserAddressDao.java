package com.viescloud.llc.venzora.dao.authentication;

import org.springframework.data.jpa.repository.JpaRepository;

import com.viescloud.llc.venzora.model.authentication.UserAddress;

public interface UserAddressDao extends JpaRepository<UserAddress, Long> {
    
}
