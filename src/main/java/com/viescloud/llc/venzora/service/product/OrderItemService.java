package com.viescloud.llc.venzora.service.product;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.OrderItemDao;
import com.viescloud.llc.venzora.model.product.OrderItem;
import com.viescloud.llc.venzora.service.VenzoraService;

@Service
public class OrderItemService extends VenzoraService<UUID, OrderItem, OrderItemDao> {

    public OrderItemService(DatabaseCall<UUID, OrderItem> databaseCall, OrderItemDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public UUID getIdFieldValue(OrderItem object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(OrderItem object, UUID id) {
        object.setId(id);
    }

}
