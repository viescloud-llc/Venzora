package com.viescloud.llc.venzora.model.product.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProductVariantStatus {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE"),
    OUT_OF_STOCK("OUT_OF_STOCK");

    private final String value;
}
