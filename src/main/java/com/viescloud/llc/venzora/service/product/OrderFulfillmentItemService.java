package com.viescloud.llc.venzora.service.product;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.OrderFulfillmentItemDao;
import com.viescloud.llc.venzora.model.product.OrderFulfillmentItem;
import com.viescloud.llc.venzora.service.VenzoraService;

@Service
public class OrderFulfillmentItemService extends VenzoraService<UUID, OrderFulfillmentItem, OrderFulfillmentItemDao> {

    public OrderFulfillmentItemService(DatabaseCall<UUID, OrderFulfillmentItem> databaseCall, OrderFulfillmentItemDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public UUID getIdFieldValue(OrderFulfillmentItem object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(OrderFulfillmentItem object, UUID id) {
        object.setId(id);
    }

}
