package com.viescloud.llc.venzora.controller.product;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.controller.ViesControllerWithUserAccess;
import com.viescloud.llc.venzora.model.product.ReturnRequest;
import com.viescloud.llc.venzora.service.product.ReturnRequestService;

@RestController
@RequestMapping("/api/v1/returns")
public class ReturnRequestController extends ViesControllerWithUserAccess<UUID, ReturnRequest, ReturnRequestService> {

    public ReturnRequestController(ReturnRequestService service) {
        super(service);
    }

}
