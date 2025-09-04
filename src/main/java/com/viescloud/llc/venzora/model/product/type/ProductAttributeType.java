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
    MULTISELECT("MULTISELECT"),
    DATE("DATE"),
    JSON("JSON");

    private final String value;
}
