package com.viescloud.llc.venzora.controller.authentication;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.viescloud.eco.viesspringutils.client.ViesOpenIdClient;
import com.viescloud.eco.viesspringutils.exception.HttpResponseThrowers;
import com.viescloud.eco.viesspringutils.model.EncodingType;
import com.viescloud.eco.viesspringutils.util.Booleans;
import com.viescloud.eco.viesspringutils.util.DataTransformationUtils;
import com.viescloud.eco.viesspringutils.util.Json;
import com.viescloud.eco.viesspringutils.util.Streams;
import com.viescloud.eco.viesspringutils.util.UUIDs;
import com.viescloud.llc.venzora.dao.authentication.UserDao;
import com.viescloud.llc.venzora.model.authentication.Oauth2LoginRequest;
import com.viescloud.llc.venzora.model.authentication.OpenIdProvider;
import com.viescloud.llc.venzora.model.authentication.User;
import com.viescloud.llc.venzora.model.authentication.UserGroup;
import com.viescloud.llc.venzora.service.authentication.JwtTokenService;
import com.viescloud.llc.venzora.service.authentication.OpenIdProviderService;
import com.viescloud.llc.venzora.service.authentication.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/authenticators")
@RequiredArgsConstructor
public class AuthenticatorController {

    private final UserService userService;
    private final JwtTokenService jwtTokenService;
    private final UserDao userDao;
    private final OpenIdProviderService openIdProviderService;
    private final RestTemplate restTemplate;

    @GetMapping("/user")
    public User getCurrentLoginUser(@RequestHeader("Authorization") String jwt) {
        return jwtTokenService.checkIsValidToken(jwt);
    }
    
    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> body) {
        var username = body.get("username");
        var password = body.get("password");
        password = DataTransformationUtils.encode(password, EncodingType.SHA256);
        var user = userService.getByUsername(username)
                              .orElseThrow(HttpResponseThrowers.throwUnauthorizedException("Invalid username or password"));

        if(username.equals(user.getUsername()) && password.equals(user.getPassword()))
            return Map.of("jwt", jwtTokenService.generateToken(user));

        return HttpResponseThrowers.throwBadRequest("Invalid username or password");
    }

    private record PasswordChangeRequest(String currentPassword, String newPassword) {}

    @PutMapping("/user/password")
    public void updateCurrentUserPassword(@RequestHeader("Authorization") String jwt, @RequestBody PasswordChangeRequest passwordChangeRequest) {
        var currentUser = jwtTokenService.checkIsValidToken(jwt);
        var encodeNewPassword = DataTransformationUtils.encode(passwordChangeRequest.newPassword, EncodingType.SHA256);
        var encodeCurrentPassword = DataTransformationUtils.encode(passwordChangeRequest.currentPassword, EncodingType.SHA256);

        var find = this.userDao.findAllByPassword(encodeCurrentPassword).stream().filter(e -> e.getId().toString().equals(currentUser.getId().toString())).findFirst();
        find.ifPresentOrElse(e -> {}, () -> HttpResponseThrowers.throwForbidden("Invalid current password"));

        currentUser.setPassword(encodeNewPassword);
        this.userService.patch(currentUser.getId(), currentUser);
    }

    private record AliasChangeRequest(String alias) {}

    @PutMapping("/user/alias")
    public void updateCurrentUserAlias(@RequestHeader("Authorization") String jwt, @RequestBody AliasChangeRequest aliasChangeRequest) {
        var currentUser = jwtTokenService.checkIsValidToken(jwt);
        this.userDao.updateAliasById(aliasChangeRequest.alias, currentUser.getId());
    }

    @PostMapping("/login/oauth2")
    public Map<String, String> loginOath2(@RequestBody Oauth2LoginRequest loginRequest) {
        if(loginRequest.getCode() == null || loginRequest.getRedirectUri() == null || loginRequest.getOpenIdProviderId() == null)
            return HttpResponseThrowers.throwBadRequest("Invalid request");

        var provider = this.openIdProviderService.getById(loginRequest.getOpenIdProviderId());
        var openIdClient = ViesOpenIdClient.<Map<String, Object>>of(restTemplate, provider.getUserInfoEndpoint(), provider.getTokenEndpoint(), provider.getClientId(), provider.getClientSecret(), true);
        try {
            var userInfo = openIdClient.getUserInfo(loginRequest.getCode(), loginRequest.getRedirectUri()).get();
            var sub = userInfo.get("sub");
            

            if(sub == null || !(sub instanceof String)) {
                return HttpResponseThrowers.throwUnauthorized("Unable to login with OpenID provider (sub is missing)");
            }

            var userOpt = this.userService.getBySub(sub.toString());
            User user = null;

            if(userOpt.isPresent()) {
                user = userOpt.get();
                if(Booleans.isTrue(provider.getAuthorizationEndpoint())) {
                    this.updateUserInfo(user, userInfo, provider);
                    user = this.userService.put(user.getId(), user);
                }
            }
            else {
                user = new User();
                user.setSub(sub.toString());
                this.updateUserInfo(user, userInfo, provider);
                user.setPassword(DataTransformationUtils.encode(UUID.randomUUID().toString(), EncodingType.SHA256));
                if(ObjectUtils.isEmpty(provider.getGroupMappings()))
                    return HttpResponseThrowers.throwUnauthorized("Unable to login with OpenID provider (group mapping is missing)");

                var groupMappings = Streams.stream(provider.getGroupMappings()).map(e -> {
                    var group = new UserGroup();
                    group.setId(e.getId());
                    return group;
                }).toArray(UserGroup[]::new);

                user.setUserGroups(Set.of(groupMappings));
                user = this.userService.post(user);
            }

            return Map.of("jwt", this.jwtTokenService.generateToken(user));
        }
        catch(Exception ex) {
            return HttpResponseThrowers.throwUnauthorized("Unable to login with OpenID provider");
        }
    }

    private void updateUserInfo(User user, Map<String, Object> userInfo, OpenIdProvider provider) {
        var alias = userInfo.get(provider.getAliasMapping());
        var email = userInfo.get(provider.getEmailMapping());
        var username = userInfo.get(provider.getUsernameMapping());

        if(alias instanceof String value && !Json.isEquals(user.getAlias(), value)) {
            user.setAlias(value);
        }

        if(email instanceof String value && !Json.isEquals(user.getEmail(), value) && isValidEmail(value)) {
            user.setEmail(value);
        }

        if(username instanceof String value && !Json.isEquals(user.getUsername(), value) && this.userService.getByUsername(value) == null) {
            user.setUsername(value);
        }

        if(ObjectUtils.isEmpty(user.getUsername()))
            user.setUsername(ObjectUtils.isEmpty(user.getSub()) ? UUIDs.random() : UUIDs.name(user.getSub()));
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }
}
