package com.viescloud.llc.venzora.controller.product;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.controller.ViesControllerWithUserAccess;
import com.viescloud.eco.viesspringutils.interfaces.annotation.RequiresAuthority;
import com.viescloud.llc.venzora.model.product.OrderFulfillment;
import com.viescloud.llc.venzora.service.product.OrderFulfillmentService;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderFulfillmentController extends ViesControllerWithUserAccess<UUID, OrderFulfillment, OrderFulfillmentService> {

    public OrderFulfillmentController(OrderFulfillmentService service) {
        super(service);
    }

    /**
     * Orders are financial records — the base class would let the row's OWNER
     * delete it, but a buyer must never be able to erase their own order.
     * PUT/PATCH are admin-gated in {@link OrderFulfillmentService}; DELETE has
     * no service-side validation hook, so the {@code orders:delete} authority
     * gate lives here (declaratively, via the lib interceptor).
     */
    @Override
    @RequiresAuthority("orders:delete")
    public ResponseEntity<HttpStatus> delete(@RequestHeader(value = "user_id", required = false) String user_id,
                                             @PathVariable("id") UUID id) {
        return super.delete(user_id, id);
    }
}
