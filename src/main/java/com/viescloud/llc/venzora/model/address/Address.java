package com.viescloud.llc.venzora.model.address;

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
public class Address {
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
    
    @Enumerated(jakarta.persistence.EnumType.STRING)
    private AddressType type;
}
