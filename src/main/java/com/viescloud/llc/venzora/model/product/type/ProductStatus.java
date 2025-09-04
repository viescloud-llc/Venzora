package com.viescloud.llc.venzora.model.product.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProductStatus {
    DRAFT("DRAFT"),
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE"),
    DISCONTINUED("DISCONTINUED");

    private final String value;
}
