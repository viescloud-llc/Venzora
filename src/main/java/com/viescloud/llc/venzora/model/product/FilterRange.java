package com.viescloud.llc.venzora.model.product;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Inclusive numeric range. Unit (currency, attribute unit) lives in the key of {@code FilterSpec.ranges}. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FilterRange {

    private BigDecimal min;
    private BigDecimal max;
}
