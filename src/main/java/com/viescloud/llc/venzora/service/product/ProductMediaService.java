package com.viescloud.llc.venzora.service.product;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.ProductMediaDao;
import com.viescloud.llc.venzora.model.product.ProductMedia;
import com.viescloud.llc.venzora.service.VenzoraService;

@Service
public class ProductMediaService extends VenzoraService<UUID, ProductMedia, ProductMediaDao> {

    public ProductMediaService(DatabaseCall<UUID, ProductMedia> databaseCall, ProductMediaDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public UUID getIdFieldValue(ProductMedia object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(ProductMedia object, UUID id) {
        object.setId(id);
    }

}
