package com.viescloud.llc.venzora.service.product;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.OrderFulfillmentDao;
import com.viescloud.llc.venzora.model.product.OrderFulfillment;
import com.viescloud.llc.venzora.service.VenzoraCustomUserAccessService;

@Service
public class OrderFulfillmentService extends VenzoraCustomUserAccessService<UUID, OrderFulfillment, OrderFulfillmentDao> {

    public OrderFulfillmentService(DatabaseCall<UUID, OrderFulfillment> databaseCall, OrderFulfillmentDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public UUID getIdFieldValue(OrderFulfillment object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(OrderFulfillment object, UUID id) {
        object.setId(id);
    }

}
