package com.viescloud.llc.venzora.controller.product;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.auto.controller.ViesAutoAdminCheckController;
import com.viescloud.llc.venzora.model.product.Review;
import com.viescloud.llc.venzora.service.product.ReviewService;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController extends ViesAutoAdminCheckController<UUID, Review, ReviewService> {

    public ReviewController(ReviewService service) {
        super(service);
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    /** Authority-based gating (permission-system.md): the seven verbs check reviews:read/create/update/delete. */
    @Override
    protected String resourceName() {
        return "reviews";
    }

}
