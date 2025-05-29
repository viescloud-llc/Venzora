package com.viescloud.llc.venzora.service.product;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.PurchaseOrderDao;
import com.viescloud.llc.venzora.model.product.PurchaseOrder;
import com.viescloud.llc.venzora.service.VenzoraService;

public class PurchaseOrderService extends VenzoraService<Long, PurchaseOrder, PurchaseOrderDao> {

    public PurchaseOrderService(DatabaseCall<Long, PurchaseOrder> databaseCall, PurchaseOrderDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public Long getIdFieldValue(PurchaseOrder object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(PurchaseOrder object, Long id) {
        object.setId(id);
    }
    
}
