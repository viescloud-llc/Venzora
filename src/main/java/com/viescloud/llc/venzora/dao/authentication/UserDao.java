package com.viescloud.llc.venzora.dao.authentication;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.viescloud.llc.venzora.model.authentication.User;

import jakarta.transaction.Transactional;

@Repository
public interface UserDao extends JpaRepository<User, Long> {
    public Optional<User> findBySub(String sub);
    public Optional<User> findByUsername(String username);
    public Optional<User> findByEmail(String email);

    public List<User> findAllBySub(String sub);
    public List<User> findAllByEmail(String email);
    public List<User> findAllByUsername(String username);
    
    public List<User> findAllByPassword(String password);

    @Modifying
    @Transactional
    @Query("UPDATE #{#entityName} u SET u.alias = :alias WHERE u.id = :id")
    long updateAliasById(@Param("alias") String alias, @Param("id") long id);
}