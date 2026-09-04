package com.viescloud.llc.venzora.model.address;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class Address implements Serializable {
    @Column(columnDefinition = "TEXT")
    private String street;
    
    @Column(columnDefinition = "TEXT")
    private String suite;
    
    @Column(columnDefinition = "TEXT")
    private String city;
    
    @Column(columnDefinition = "TEXT")
    private String state;
    
    @Column(columnDefinition = "TEXT")
    private String postalCode;
    
    @Column(columnDefinition = "TEXT")
    private String country;

    /** Optional sub-city/administrative district (used by addresses in e.g. VN, TW, KR, IN). */
    @Column(columnDefinition = "TEXT")
    private String district;
    
    @Enumerated(jakarta.persistence.EnumType.STRING)
    private AddressType type;
}
