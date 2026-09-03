package com.viescloud.llc.venzora.model.product;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for {@code POST /api/v1/products/{id}/generate-variants}.
 *
 * <p>Each axis names a SELECT-type {@link AttributeDefinition} and (optionally)
 * the subset of its options to combine; an omitted/empty {@code optionIds}
 * means "every option of that definition". The generator builds the cartesian
 * product of all axes — e.g. Size {S,M,L} × Color {Red,Blue} → 6 variants.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenerateVariantsRequest {

    private List<Axis> axes;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Axis {
        private UUID attributeDefinitionId;
        /** Options to include; null/empty = all options of the definition. */
        private List<UUID> optionIds;
    }
}
