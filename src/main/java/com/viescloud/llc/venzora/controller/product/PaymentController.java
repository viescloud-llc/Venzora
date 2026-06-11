package com.viescloud.llc.venzora.controller.product;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.auto.controller.ViesAutoAdminCheckController;
import com.viescloud.llc.venzora.model.product.Payment;
import com.viescloud.llc.venzora.service.product.PaymentService;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController extends ViesAutoAdminCheckController<UUID, Payment, PaymentService> {

    public PaymentController(PaymentService service) {
        super(service);
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

}
