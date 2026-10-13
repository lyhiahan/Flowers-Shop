package com.finalproject.service;

import com.finalproject.entity.Review;
import java.util.List;

public interface ReviewService {
    Review addReview(Long userId, Long productId, Long orderId, int rating, String content, String imageUrl);
    Review addReview(Long userId, Long productId, Long orderId, int rating, String content, String imageUrl,
                     org.springframework.web.multipart.MultipartFile imageFile) throws java.io.IOException;
    List<Review> getReviewsByProduct(Long productId);
    boolean hasReviewed(Long userId, Long productId, Long orderId);
    boolean canReview(Long userId, Long productId);
    Long getEligibleOrderId(Long userId, Long productId);
}
