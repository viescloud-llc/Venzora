package com.viescloud.llc.venzora.service.product;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.ShipmentDao;
import com.viescloud.llc.venzora.model.product.Shipment;
import com.viescloud.llc.venzora.service.VenzoraService;

@Service
public class ShipmentService extends VenzoraService<UUID, Shipment, ShipmentDao> {

    public ShipmentService(DatabaseCall<UUID, Shipment> databaseCall, ShipmentDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public UUID getIdFieldValue(Shipment object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(Shipment object, UUID id) {
        object.setId(id);
    }

}
