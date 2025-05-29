package com.viescloud.llc.venzora.model.authentication;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.viescloud.eco.viesspringutils.config.jpa.BooleanConverter;
import com.viescloud.eco.viesspringutils.config.jpa.StringHexEncodeConverter;
import com.viescloud.eco.viesspringutils.model.UserAccess;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@JsonInclude(Include.NON_NULL)
public class OpenIdProvider extends UserAccess {

    @Id
    @GeneratedValue
    private Integer id;

    @Column(columnDefinition = "TEXT")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT", nullable = false)
    @Convert(converter = StringHexEncodeConverter.class)
    private String clientId;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    @Convert(converter = StringHexEncodeConverter.class)
    private String clientSecret;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String authorizationEndpoint;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String tokenEndpoint;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String userInfoEndpoint;
    
    @Column(columnDefinition = "TEXT")
    private String endSessionEndpoint;
    
    @Column(columnDefinition = "TEXT")
    private String aliasMapping;
    
    @Column(columnDefinition = "TEXT")
    private String emailMapping;
    
    @Column(columnDefinition = "TEXT")
    private String usernameMapping;
    
    @ManyToMany(cascade = CascadeType.REFRESH, fetch = FetchType.EAGER)
    private Set<UserGroup> groupMappings;

    @Column(length = 20)
    @Convert(converter = BooleanConverter.class)
    private Boolean autoUpdateUserInfo;
}
