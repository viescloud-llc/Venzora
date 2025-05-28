package com.viescloud.llc.venzora.model.product;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentMethodtype {
    CARD("CARD"),
    CASH("CASH"),
    PAYPAL("PAYPAL");

    private final String value;
}
