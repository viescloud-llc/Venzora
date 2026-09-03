package com.viescloud.llc.venzora.controller.checkout;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.interfaces.annotation.RequiresUser;
import org.springframework.web.server.ResponseStatusException;

import com.viescloud.llc.venzora.model.checkout.CheckoutStartRequest;
import com.viescloud.llc.venzora.model.checkout.CheckoutStartResponse;
import com.viescloud.llc.venzora.model.product.OrderFulfillment;
import com.viescloud.llc.venzora.service.checkout.CheckoutOrchestratorService;

/**
 * Server-side checkout orchestration. Coexists with {@code OrderFulfillmentController}
 * at the same base path; routes by full URL.
 */
@RequiresUser
@RestController
@RequestMapping("/api/v1/orders")
public class CheckoutOrchestratorController {

    private final CheckoutOrchestratorService orchestrator;

    public CheckoutOrchestratorController(CheckoutOrchestratorService orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/checkout")
    public CheckoutStartResponse start(
            @RequestHeader(value = "user_id", required = false) String userIdHeader,
            @RequestBody CheckoutStartRequest req) {
        UUID buyerId = requireUserId(userIdHeader);
        return orchestrator.start(req, buyerId);
    }

    @PostMapping("/{id}/complete")
    public OrderFulfillment complete(
            @RequestHeader(value = "user_id", required = false) String userIdHeader,
            @PathVariable UUID id) {
        UUID buyerId = requireUserId(userIdHeader);
        return orchestrator.complete(id, buyerId);
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
