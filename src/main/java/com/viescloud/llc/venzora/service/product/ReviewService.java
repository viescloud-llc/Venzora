package com.viescloud.llc.venzora.service.product;

import org.springframework.stereotype.Service;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.dao.product.ReviewDao;
import com.viescloud.llc.venzora.model.product.Review;
import com.viescloud.llc.venzora.service.VenzoraService;

@Service
public class ReviewService extends VenzoraService<Long, Review, ReviewDao> {

    public ReviewService(DatabaseCall<Long, Review> databaseCall, ReviewDao repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    @Override
    public Long getIdFieldValue(Review object) {
        return object.getId();
    }

    @Override
    public void setIdFieldValue(Review object, Long id) {
        object.setId(id);
    }
    
}
