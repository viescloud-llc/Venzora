package com.viescloud.llc.venzora.model.authentication;

import com.viescloud.eco.viesspringutils.config.jpa.BooleanConverter;
import com.viescloud.eco.viesspringutils.model.TrackedTimeStamp;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class UserInfo extends TrackedTimeStamp {

    @Id
    private Long userId;
    
    @Column(columnDefinition = "TEXT")
    private String firstName;
    
    @Column(columnDefinition = "TEXT")
    private String lastName;
    
    @Column(columnDefinition = "TEXT")
    private String phoneNumber;
    
    @Column(columnDefinition = "TEXT")
    private String avatarUrl;
    
    @Column(length = 20)
    @Convert(converter = BooleanConverter.class)
    private Boolean verified;
    
    @Column(length = 20)
    @Convert(converter = BooleanConverter.class)
    private Boolean inactive;
}
