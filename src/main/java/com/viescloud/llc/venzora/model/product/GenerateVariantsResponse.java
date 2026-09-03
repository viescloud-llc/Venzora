package com.viescloud.llc.venzora.model.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of a variant generation run: how many combinations were created, how
 * many were skipped (SKU already present locally or taken globally), and the
 * updated product graph so the client can refresh in place without a re-fetch.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenerateVariantsResponse {
    private int created;
    private int skipped;
    private Product product;
}
