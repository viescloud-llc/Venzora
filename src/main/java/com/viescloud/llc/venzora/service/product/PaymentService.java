package com.viescloud.llc.venzora.service.product;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.PaymentDao;
import com.viescloud.llc.venzora.model.product.Payment;
import com.viescloud.llc.venzora.service.VenzoraService;

public class PaymentService extends VenzoraService<Long, Payment, PaymentDao> {

    public PaymentService(DatabaseCall<Long, Payment> databaseCall, PaymentDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public Long getIdFieldValue(Payment object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(Payment object, Long id) {
        object.setId(id);
    }
}
