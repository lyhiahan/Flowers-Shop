package com.finalproject.service.impl;

import com.finalproject.entity.*;
import com.finalproject.enums.OrderStatus;
import com.finalproject.repository.*;
import com.finalproject.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;

    @Override
    @Transactional
    public Review addReview(Long userId, Long productId, Long orderId, int rating, String content, String imageUrl) {
        // 1. Kiểm tra validation text tối thiểu 50 ký tự theo yêu cầu đề cương
        if (content == null || content.trim().length() < 50) {
            throw new IllegalArgumentException("Bình luận đánh giá sản phẩm bắt buộc phải có tối thiểu 50 ký tự!");
        }

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Số sao đánh giá phải từ 1 đến 5 sao!");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"));

        Order order = null;
        if (orderId != null) {
            order = orderRepository.findById(orderId).orElse(null);
            if (order != null) {
                // Kiểm tra đơn hàng đã giao thành công chưa
                if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.COMPLETED) {
                    throw new IllegalStateException("Bạn chỉ có thể đánh giá sản phẩm sau khi đơn hàng đã được giao thành công!");
                }
            }
        }

        Review review = Review.builder()
                .user(user)
                .product(product)
                .store(product.getStore())
                .order(order)
                .rating(rating)
                .content(content.trim())
                .imageUrl(imageUrl)
                .build();

        Review saved = reviewRepository.save(review);

        // Cập nhật rating trung bình của sản phẩm (Post-condition UC_014)
        List<Review> allReviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
        double avg = allReviews.stream().mapToInt(Review::getRating).average().orElse(5.0);
        product.setRating(Math.round(avg * 10.0) / 10.0);
        productRepository.save(product);

        // Cập nhật rating trung bình của cửa hàng (Post-condition UC_014)
        if (product.getStore() != null) {
            Store store = product.getStore();
            List<Review> storeReviews = reviewRepository.findByStoreIdOrderByCreatedAtDesc(store.getId());
            double storeAvg = storeReviews.stream().mapToInt(Review::getRating).average().orElse(5.0);
            store.setRating(Math.round(storeAvg * 10.0) / 10.0);
            storeRepository.save(store);
        }

        return saved;
    }

    @Override
    public List<Review> getReviewsByProduct(Long productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    @Override
    public boolean hasReviewed(Long userId, Long productId, Long orderId) {
        if (userId == null || productId == null || orderId == null) return false;
        return reviewRepository.existsByUserIdAndProductIdAndOrderId(userId, productId, orderId);
    }
}
