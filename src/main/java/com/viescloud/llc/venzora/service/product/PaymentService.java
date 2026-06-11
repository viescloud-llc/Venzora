package com.viescloud.llc.venzora.service.product;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.PaymentDao;
import com.viescloud.llc.venzora.model.product.Payment;
import com.viescloud.llc.venzora.service.VenzoraService;

@Service
public class PaymentService extends VenzoraService<UUID, Payment, PaymentDao> {

    public PaymentService(DatabaseCall<UUID, Payment> databaseCall, PaymentDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public UUID getIdFieldValue(Payment object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(Payment object, UUID id) {
        object.setId(id);
    }
}
