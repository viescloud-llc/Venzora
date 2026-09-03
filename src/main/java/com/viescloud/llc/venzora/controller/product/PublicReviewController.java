package com.viescloud.llc.venzora.controller.product;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.interfaces.annotation.PublicEndpoint;

import com.viescloud.eco.viesspringutils.model.PageResponse;
import com.viescloud.llc.venzora.dao.product.ReviewDao;
import com.viescloud.llc.venzora.model.product.Review;

/**
 * Public, unauthenticated reviews list for a product. Used by the storefront's
 * product-detail page to render the existing reviews block.
 */
@PublicEndpoint("Unauthenticated storefront review reads")
@RestController
@RequestMapping("/api/v1/public/products")
public class PublicReviewController {

    private final ReviewDao reviewDao;

    public PublicReviewController(ReviewDao reviewDao) {
        this.reviewDao = reviewDao;
    }

    @GetMapping("/{productId}/reviews")
    public PageResponse<Review> reviewsForProduct(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        var pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.fromString(sortDir), sort));
        Page<Review> result = reviewDao.findAllByProductId(productId, pageable);
        return PageResponse.of(result);
    }
}
