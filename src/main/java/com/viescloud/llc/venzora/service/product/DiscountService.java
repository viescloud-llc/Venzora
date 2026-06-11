package com.viescloud.llc.venzora.service.product;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.DiscountDao;
import com.viescloud.llc.venzora.model.product.Discount;
import com.viescloud.llc.venzora.service.VenzoraService;

@Service
public class DiscountService extends VenzoraService<UUID, Discount, DiscountDao> {

    public DiscountService(DatabaseCall<UUID, Discount> databaseCall, DiscountDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public UUID getIdFieldValue(Discount object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(Discount object, UUID id) {
        object.setId(id);
    }

}
