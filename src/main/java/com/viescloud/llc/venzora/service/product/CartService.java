package com.viescloud.llc.venzora.service.product;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.CartDao;
import com.viescloud.llc.venzora.model.product.Cart;
import com.viescloud.llc.venzora.service.VenzoraCustomUserAccessService;

@Service
public class CartService extends VenzoraCustomUserAccessService<UUID, Cart, CartDao> {

    public CartService(DatabaseCall<UUID, Cart> databaseCall, CartDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public UUID getIdFieldValue(Cart object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(Cart object, UUID id) {
        object.setId(id);
    }

}
