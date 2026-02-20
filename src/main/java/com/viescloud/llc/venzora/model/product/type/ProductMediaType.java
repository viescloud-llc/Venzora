package com.viescloud.llc.venzora.model.product.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProductMediaType {
    IMAGE("IMAGE"),
    VIDEO("VIDEO");

    private final String value;    
}
