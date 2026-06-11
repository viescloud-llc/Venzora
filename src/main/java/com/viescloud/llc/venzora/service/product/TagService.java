package com.viescloud.llc.venzora.service.product;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.TagDao;
import com.viescloud.llc.venzora.model.product.Tag;
import com.viescloud.llc.venzora.service.VenzoraService;

@Service
public class TagService extends VenzoraService<UUID, Tag, TagDao> {

    public TagService(DatabaseCall<UUID, Tag> databaseCall, TagDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public UUID getIdFieldValue(Tag object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(Tag object, UUID id) {
        object.setId(id);
    }
    
}
