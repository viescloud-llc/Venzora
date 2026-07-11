package com.viescloud.llc.venzora.controller.me;

import java.util.HashSet;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.llc.venzora.dao.authentication.UserAddressDao;
import com.viescloud.llc.venzora.model.authentication.UserAddress;
import com.viescloud.llc.venzora.util.UserIdHeader;

/**
 * Self-service saved-addresses endpoints. {@link UserAddress} holds a single
 * {@code Set<Address>} per user. To add or remove an entry, the frontend PUTs the
 * whole row with the updated set.
 */
@RestController
@RequestMapping("/api/v1/me/addresses")
public class MyUserAddressController {

    private final UserAddressDao userAddressDao;

    public MyUserAddressController(UserAddressDao userAddressDao) {
        this.userAddressDao = userAddressDao;
    }

    /**
     * Returns the caller's UserAddress. Auto-creates an empty row in memory if none
     * exists yet — convenient for the storefront's account page so it can render
     * "add your first address" without a separate POST.
     */
    @GetMapping
    public UserAddress get(@RequestHeader(value = "user_id", required = false) String userIdHeader) {
        UUID userId = UserIdHeader.require(userIdHeader);
        return userAddressDao.findById(userId).orElseGet(() -> {
            UserAddress empty = new UserAddress();
            empty.setUserId(userId);
            empty.setAddresses(new HashSet<>());
            return empty;
        });
    }

    /** Upsert — creates the row if it doesn't exist, replaces the full address set otherwise. */
    @PutMapping
    public UserAddress upsert(
            @RequestHeader(value = "user_id", required = false) String userIdHeader,
            @RequestBody UserAddress body) {
        UUID userId = UserIdHeader.require(userIdHeader);
        body.setUserId(userId);
        if (body.getAddresses() == null) {
            body.setAddresses(new HashSet<>());
        }
        return userAddressDao.save(body);
    }
}
