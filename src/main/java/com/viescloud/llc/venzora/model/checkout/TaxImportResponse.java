package com.viescloud.llc.venzora.model.checkout;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaxImportResponse {

    private int imported;
    private int replaced;     // count deleted when mode=replace; 0 otherwise
    private String mode;      // "append" | "replace"
}
