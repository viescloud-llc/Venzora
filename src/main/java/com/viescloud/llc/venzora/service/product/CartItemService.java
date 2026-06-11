package com.viescloud.llc.venzora.service.product;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.CartItemDao;
import com.viescloud.llc.venzora.model.product.CartItem;
import com.viescloud.llc.venzora.service.VenzoraService;

@Service
public class CartItemService extends VenzoraService<UUID, CartItem, CartItemDao> {

    public CartItemService(DatabaseCall<UUID, CartItem> databaseCall, CartItemDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public UUID getIdFieldValue(CartItem object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(CartItem object, UUID id) {
        object.setId(id);
    }

}
