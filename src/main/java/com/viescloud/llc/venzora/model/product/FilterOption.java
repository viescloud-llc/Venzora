package com.viescloud.llc.venzora.model.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A single choice in a SELECT / MULTI_SELECT filter. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FilterOption {

    private String value;          // sent back as the query param value
    private String displayValue;   // shown in the UI; falls back to value when null
}
