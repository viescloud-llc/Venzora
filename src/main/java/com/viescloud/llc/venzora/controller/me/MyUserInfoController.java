package com.viescloud.llc.venzora.controller.me;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.interfaces.annotation.RequiresUser;
import org.springframework.web.server.ResponseStatusException;

import com.viescloud.llc.venzora.dao.authentication.UserInfoDao;
import com.viescloud.llc.venzora.model.authentication.UserInfo;
import com.viescloud.llc.venzora.util.UserIdHeader;

/**
 * Self-service profile endpoints. {@link UserInfo} keys on {@code userId} (the
 * buyer's UUID), so there is exactly one row per user; PUT acts as upsert.
 */
@RequiresUser
@RestController
@RequestMapping("/api/v1/me/info")
public class MyUserInfoController {

    private final UserInfoDao userInfoDao;

    public MyUserInfoController(UserInfoDao userInfoDao) {
        this.userInfoDao = userInfoDao;
    }

    /** Returns the caller's UserInfo. 404 if the row hasn't been created yet. */
    @GetMapping
    public UserInfo get(@RequestHeader(value = "user_id", required = false) String userIdHeader) {
        UUID userId = UserIdHeader.require(userIdHeader);
        return userInfoDao.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
    }

    /** Upsert — creates the row if it doesn't exist, replaces it otherwise. */
    @PutMapping
    public UserInfo upsert(
            @RequestHeader(value = "user_id", required = false) String userIdHeader,
            @RequestBody UserInfo body) {
        UUID userId = UserIdHeader.require(userIdHeader);
        body.setUserId(userId);
        return userInfoDao.save(body);
    }

    /** Partial update — merges body fields into the existing row. Creates if missing. */
    @PatchMapping
    public UserInfo patch(
            @RequestHeader(value = "user_id", required = false) String userIdHeader,
            @RequestBody UserInfo body) {
        UUID userId = UserIdHeader.require(userIdHeader);
        UserInfo existing = userInfoDao.findById(userId).orElseGet(() -> {
            UserInfo fresh = new UserInfo();
            fresh.setUserId(userId);
            return fresh;
        });
        if (body.getFirstName() != null) existing.setFirstName(body.getFirstName());
        if (body.getLastName() != null) existing.setLastName(body.getLastName());
        if (body.getPhoneNumber() != null) existing.setPhoneNumber(body.getPhoneNumber());
        if (body.getAvatarUrl() != null) existing.setAvatarUrl(body.getAvatarUrl());
        if (body.getVerified() != null) existing.setVerified(body.getVerified());
        if (body.getInactive() != null) existing.setInactive(body.getInactive());
        return userInfoDao.save(existing);
    }
}
