package com.viescloud.llc.venzora.model.product.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProductAttributeType {
    TEXT("TEXT"),
    NUMBER("NUMBER"),
    BOOLEAN("BOOLEAN"),
    SELECT("SELECT"),
    MULTI_SELECT("MULTI_SELECT"),
    DATE("DATE"),
    TIME("TIME"),
    DATE_TIME("DATE_TIME");

    private final String value;
}
