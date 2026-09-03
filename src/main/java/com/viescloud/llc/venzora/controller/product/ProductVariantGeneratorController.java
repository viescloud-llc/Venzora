package com.viescloud.llc.venzora.controller.product;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.interfaces.annotation.RequiresAuthority;
import com.viescloud.llc.venzora.model.product.GenerateVariantsRequest;
import com.viescloud.llc.venzora.model.product.GenerateVariantsResponse;
import com.viescloud.llc.venzora.service.product.ProductVariantGeneratorService;

import lombok.RequiredArgsConstructor;

/**
 * Hand-written endpoint next to the CRUD {@link ProductController} on the same
 * base path (Spring routes by full URL + verb — same pattern as the checkout
 * orchestrator on /api/v1/orders).
 *
 * <p>Gated on {@code catalog:update} — same authority the catalog CRUD verbs
 * check, enforced declaratively by the lib's {@code @RequiresAuthority}.
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductVariantGeneratorController {

    private final ProductVariantGeneratorService generatorService;

    @RequiresAuthority("catalog:update")
    @PostMapping("/{id}/generate-variants")
    public GenerateVariantsResponse generate(
            @PathVariable("id") UUID productId,
            @RequestBody GenerateVariantsRequest request) {
        return generatorService.generate(productId, request);
    }
}
