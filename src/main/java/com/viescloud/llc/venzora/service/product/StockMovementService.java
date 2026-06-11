package com.viescloud.llc.venzora.service.product;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.StockMovementDao;
import com.viescloud.llc.venzora.model.product.StockMovement;
import com.viescloud.llc.venzora.service.VenzoraService;

@Service
public class StockMovementService extends VenzoraService<UUID, StockMovement, StockMovementDao> {

    public StockMovementService(DatabaseCall<UUID, StockMovement> databaseCall, StockMovementDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public UUID getIdFieldValue(StockMovement object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(StockMovement object, UUID id) {
        object.setId(id);
    }

}
