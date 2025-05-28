package com.viescloud.llc.venzora.dao.authentication;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.viescloud.llc.venzora.model.authentication.UserGroup;


@Repository
public interface UserGroupDao extends JpaRepository<UserGroup, Integer> {
    public Optional<UserGroup> findById(int id);
    public Optional<UserGroup> findByName(String name);

    public List<UserGroup> findAllByName(String name);
}
