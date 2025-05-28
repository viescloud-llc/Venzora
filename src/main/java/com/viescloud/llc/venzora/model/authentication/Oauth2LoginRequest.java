package com.viescloud.llc.venzora.model.authentication;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Oauth2LoginRequest {
    private String code;
    private String redirectUri;
    private Integer openIdProviderId;
}
