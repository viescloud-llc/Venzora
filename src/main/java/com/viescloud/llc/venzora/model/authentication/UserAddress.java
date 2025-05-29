package com.viescloud.llc.venzora.model.authentication;

import java.util.HashSet;
import java.util.Set;

import com.viescloud.eco.viesspringutils.model.TrackedTimeStamp;
import com.viescloud.llc.venzora.model.address.Address;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
public class UserAddress extends TrackedTimeStamp {
    
    @Id
    private Long userId;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<Address> addresses = new HashSet<>();
}
