package com.viescloud.llc.venzora.controller.checkout;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.viescloud.llc.venzora.model.checkout.DiscountValidationRequest;
import com.viescloud.llc.venzora.model.checkout.DiscountValidationResponse;
import com.viescloud.llc.venzora.service.checkout.CheckoutOrchestratorService;

/**
 * Non-destructive coupon preview. Coexists with the admin-gated CRUD
 * {@code DiscountController} at the same base path; routes by full URL + verb.
 */
@RestController
@RequestMapping("/api/v1/discounts")
public class DiscountValidationController {

    private final CheckoutOrchestratorService orchestrator;

    public DiscountValidationController(CheckoutOrchestratorService orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/validate")
    public DiscountValidationResponse validate(
            @RequestHeader(value = "user_id", required = false) String userIdHeader,
            @RequestBody DiscountValidationRequest req) {
        UUID buyerId = requireUserId(userIdHeader);
        return orchestrator.validateDiscountForCart(req.getCode(), req.getCartId(), buyerId);
    }

    private static UUID requireUserId(String header) {
        if (header == null || header.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user_id header required");
        }
        try {
            return UUID.fromString(header);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "user_id must be a UUID");
        }
    }
}
