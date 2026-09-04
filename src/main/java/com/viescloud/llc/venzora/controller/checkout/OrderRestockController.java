package com.viescloud.llc.venzora.controller.checkout;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.interfaces.annotation.CurrentUserId;
import com.viescloud.eco.viesspringutils.interfaces.annotation.RequiresAuthority;
import com.viescloud.llc.venzora.model.product.OrderFulfillment;
import com.viescloud.llc.venzora.model.product.RestockRequest;
import com.viescloud.llc.venzora.service.checkout.OrderRestockService;

import lombok.RequiredArgsConstructor;

/**
 * Back-office restock after a refund/return. Shares the {@code /api/v1/orders}
 * base path with the CRUD controller (routing by full URL + verb). Gated on
 * {@code orders:restock} (finance / orders-wide roles) OR {@code inventory:update}
 * (inventory admins) — either may put goods back on the shelf.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderRestockController {

    private final OrderRestockService restockService;

    @RequiresAuthority({"orders:restock", "inventory:update"})
    @PostMapping("/{id}/restock")
    public OrderFulfillment restock(@CurrentUserId UUID userId,
                                    @PathVariable("id") UUID orderId,
                                    @RequestBody RestockRequest request) {
        return restockService.restock(orderId, request, userId);
    }
}
