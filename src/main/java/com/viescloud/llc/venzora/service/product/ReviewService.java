package com.viescloud.llc.venzora.service.product;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.ReviewDao;
import com.viescloud.llc.venzora.model.product.Review;
import com.viescloud.llc.venzora.service.VenzoraService;

@Service
public class ReviewService extends VenzoraService<UUID, Review, ReviewDao> {

    public ReviewService(DatabaseCall<UUID, Review> databaseCall, ReviewDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public UUID getIdFieldValue(Review object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(Review object, UUID id) {
        object.setId(id);
    }
    
}
