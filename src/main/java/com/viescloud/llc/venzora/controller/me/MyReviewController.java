package com.viescloud.llc.venzora.controller.me;

import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.interfaces.annotation.RequiresUser;
import org.springframework.web.server.ResponseStatusException;

import com.viescloud.eco.viesspringutils.model.PageResponse;
import com.viescloud.llc.venzora.dao.product.ReviewDao;
import com.viescloud.llc.venzora.model.product.Review;
import com.viescloud.llc.venzora.util.UserIdHeader;

/**
 * Self-service review endpoints. The buyer's UUID is taken from the {@code user_id}
 * header; every operation is filtered/forced to that user — {@code Review.userId} is
 * server-stamped on create and verified on update / delete.
 */
@RequiresUser
@RestController
@RequestMapping("/api/v1/me/reviews")
public class MyReviewController {

    private final ReviewDao reviewDao;

    public MyReviewController(ReviewDao reviewDao) {
        this.reviewDao = reviewDao;
    }

    @GetMapping
    public PageResponse<Review> list(
            @RequestHeader(value = "user_id", required = false) String userIdHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        UUID userId = UserIdHeader.require(userIdHeader);
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        var pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.fromString(sortDir), sort));
        Page<Review> result = reviewDao.findAllByUserId(userId, pageable);
        return PageResponse.of(result);
    }

    @PostMapping
    public Review create(
            @RequestHeader(value = "user_id", required = false) String userIdHeader,
            @RequestBody Review body) {
        UUID userId = UserIdHeader.require(userIdHeader);
        if (body.getProductId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId is required");
        }
        if (body.getRating() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rating is required");
        }
        body.setId(null);
        body.setUserId(userId);
        body.setCreatedAt(null);
        body.setUpdatedAt(null);
        return reviewDao.save(body);
    }

    @PutMapping("/{id}")
    public Review update(
            @RequestHeader(value = "user_id", required = false) String userIdHeader,
            @PathVariable UUID id,
            @RequestBody Review body) {
        UUID userId = UserIdHeader.require(userIdHeader);
        Review existing = ownedByCaller(id, userId);
        if (body.getComment() != null) existing.setComment(body.getComment());
        if (body.getRating() != null) existing.setRating(body.getRating());
        return reviewDao.save(existing);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader(value = "user_id", required = false) String userIdHeader,
            @PathVariable UUID id) {
        UUID userId = UserIdHeader.require(userIdHeader);
        ownedByCaller(id, userId);
        reviewDao.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private Review ownedByCaller(UUID reviewId, UUID userId) {
        Review existing = reviewDao.findById(reviewId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found: " + reviewId));
        if (!Objects.equals(existing.getUserId(), userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Review does not belong to the authenticated user");
        }
        return existing;
    }
}
