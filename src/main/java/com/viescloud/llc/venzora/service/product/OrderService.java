package com.viescloud.llc.venzora.service.product;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.OrderDao;
import com.viescloud.llc.venzora.model.product.Order;
import com.viescloud.llc.venzora.service.VenzoraCustomUserAccessService;

@Service
public class OrderService extends VenzoraCustomUserAccessService<UUID, Order, OrderDao> {

    public OrderService(DatabaseCall<UUID, Order> databaseCall, OrderDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public UUID getIdFieldValue(Order object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(Order object, UUID id) {
        object.setId(id);
    }

}
