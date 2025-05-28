package com.viescloud.llc.venzora.model.address;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AddressType {
    BILLING("BILLING"),
    SHIPPING("SHIPPING");

    private final String value;
}
