package com.viescloud.llc.venzora.service.product;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.ReturnRequestDao;
import com.viescloud.llc.venzora.model.product.ReturnRequest;
import com.viescloud.llc.venzora.service.VenzoraCustomUserAccessService;

@Service
public class ReturnRequestService extends VenzoraCustomUserAccessService<UUID, ReturnRequest, ReturnRequestDao> {

    public ReturnRequestService(DatabaseCall<UUID, ReturnRequest> databaseCall, ReturnRequestDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public UUID getIdFieldValue(ReturnRequest object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(ReturnRequest object, UUID id) {
        object.setId(id);
    }

}
