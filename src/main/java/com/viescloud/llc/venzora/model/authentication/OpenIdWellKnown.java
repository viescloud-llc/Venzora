package com.viescloud.llc.venzora.model.authentication;

import com.fasterxml.jackson.annotation.*;
import java.util.List;

@lombok.Data
public class OpenIdWellKnown {
    @lombok.Getter(onMethod_ = {@JsonProperty("issuer")})
    @lombok.Setter(onMethod_ = {@JsonProperty("issuer")})
    private String issuer;
    @lombok.Getter(onMethod_ = {@JsonProperty("authorization_endpoint")})
    @lombok.Setter(onMethod_ = {@JsonProperty("authorization_endpoint")})
    private String authorizationEndpoint;
    @lombok.Getter(onMethod_ = {@JsonProperty("token_endpoint")})
    @lombok.Setter(onMethod_ = {@JsonProperty("token_endpoint")})
    private String tokenEndpoint;
    @lombok.Getter(onMethod_ = {@JsonProperty("userinfo_endpoint")})
    @lombok.Setter(onMethod_ = {@JsonProperty("userinfo_endpoint")})
    private String userinfoEndpoint;
    @lombok.Getter(onMethod_ = {@JsonProperty("end_session_endpoint")})
    @lombok.Setter(onMethod_ = {@JsonProperty("end_session_endpoint")})
    private String endSessionEndpoint;
    @lombok.Getter(onMethod_ = {@JsonProperty("introspection_endpoint")})
    @lombok.Setter(onMethod_ = {@JsonProperty("introspection_endpoint")})
    private String introspectionEndpoint;
    @lombok.Getter(onMethod_ = {@JsonProperty("revocation_endpoint")})
    @lombok.Setter(onMethod_ = {@JsonProperty("revocation_endpoint")})
    private String revocationEndpoint;
    @lombok.Getter(onMethod_ = {@JsonProperty("device_authorization_endpoint")})
    @lombok.Setter(onMethod_ = {@JsonProperty("device_authorization_endpoint")})
    private String deviceAuthorizationEndpoint;
    @lombok.Getter(onMethod_ = {@JsonProperty("response_types_supported")})
    @lombok.Setter(onMethod_ = {@JsonProperty("response_types_supported")})
    private List<String> responseTypesSupported;
    @lombok.Getter(onMethod_ = {@JsonProperty("response_modes_supported")})
    @lombok.Setter(onMethod_ = {@JsonProperty("response_modes_supported")})
    private List<String> responseModesSupported;
    @lombok.Getter(onMethod_ = {@JsonProperty("jwks_uri")})
    @lombok.Setter(onMethod_ = {@JsonProperty("jwks_uri")})
    private String jwksURI;
    @lombok.Getter(onMethod_ = {@JsonProperty("grant_types_supported")})
    @lombok.Setter(onMethod_ = {@JsonProperty("grant_types_supported")})
    private List<String> grantTypesSupported;
    @lombok.Getter(onMethod_ = {@JsonProperty("id_token_signing_alg_values_supported")})
    @lombok.Setter(onMethod_ = {@JsonProperty("id_token_signing_alg_values_supported")})
    private List<String> idTokenSigningAlgValuesSupported;
    @lombok.Getter(onMethod_ = {@JsonProperty("subject_types_supported")})
    @lombok.Setter(onMethod_ = {@JsonProperty("subject_types_supported")})
    private List<String> subjectTypesSupported;
    @lombok.Getter(onMethod_ = {@JsonProperty("token_endpoint_auth_methods_supported")})
    @lombok.Setter(onMethod_ = {@JsonProperty("token_endpoint_auth_methods_supported")})
    private List<String> tokenEndpointAuthMethodsSupported;
    @lombok.Getter(onMethod_ = {@JsonProperty("acr_values_supported")})
    @lombok.Setter(onMethod_ = {@JsonProperty("acr_values_supported")})
    private List<String> acrValuesSupported;
    @lombok.Getter(onMethod_ = {@JsonProperty("scopes_supported")})
    @lombok.Setter(onMethod_ = {@JsonProperty("scopes_supported")})
    private List<String> scopesSupported;
    @lombok.Getter(onMethod_ = {@JsonProperty("request_parameter_supported")})
    @lombok.Setter(onMethod_ = {@JsonProperty("request_parameter_supported")})
    private Boolean requestParameterSupported;
    @lombok.Getter(onMethod_ = {@JsonProperty("claims_supported")})
    @lombok.Setter(onMethod_ = {@JsonProperty("claims_supported")})
    private List<String> claimsSupported;
    @lombok.Getter(onMethod_ = {@JsonProperty("claims_parameter_supported")})
    @lombok.Setter(onMethod_ = {@JsonProperty("claims_parameter_supported")})
    private Boolean claimsParameterSupported;
    @lombok.Getter(onMethod_ = {@JsonProperty("code_challenge_methods_supported")})
    @lombok.Setter(onMethod_ = {@JsonProperty("code_challenge_methods_supported")})
    private List<String> codeChallengeMethodsSupported;
}
