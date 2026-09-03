package com.viescloud.llc.venzora.controller.product;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.viescloud.eco.viesspringutils.auto.config.ViesPermission;
import com.viescloud.llc.venzora.model.product.GenerateVariantsRequest;
import com.viescloud.llc.venzora.model.product.GenerateVariantsResponse;
import com.viescloud.llc.venzora.service.product.ProductVariantGeneratorService;
import com.viescloud.llc.venzora.util.UserIdHeader;

import lombok.RequiredArgsConstructor;

/**
 * Hand-written endpoint next to the CRUD {@link ProductController} on the same
 * base path (Spring routes by full URL + verb — same pattern as the checkout
 * orchestrator on /api/v1/orders).
 *
 * <p>Admin-gated to match the rest of the catalog surface: the caller's
 * {@code user_id} must resolve to an existing user in the ADMIN group, exactly
 * like the CRUD verbs on {@code ViesAutoAdminCheckController}.
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductVariantGeneratorController {

    private final ProductVariantGeneratorService generatorService;
    private final ViesPermission viesPermission;

    @PostMapping("/{id}/generate-variants")
    public GenerateVariantsResponse generate(
            @RequestHeader(value = "user_id", required = false) String userIdHeader,
            @PathVariable("id") UUID productId,
            @RequestBody GenerateVariantsRequest request) {
        UUID userId = UserIdHeader.require(userIdHeader);
        if (!viesPermission.hasAdminPermission(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin permission required");
        }
        return generatorService.generate(productId, request);
    }
}
