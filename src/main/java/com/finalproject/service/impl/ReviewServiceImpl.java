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
    private final OrderDetailRepository orderDetailRepository;
    private final StoreRepository storeRepository;
    private final jakarta.persistence.EntityManager entityManager;

    @Override
    @Transactional
    public Review addReview(Long userId, Long productId, Long orderId, int rating, String content, String imageUrl) {
        // 0. BẮT BUỘC: Khách hàng chỉ được đánh giá khi và chỉ khi đã đặt hàng và nhận được hàng rồi
        if (!canReview(userId, productId)) {
            throw new IllegalStateException("Chức năng đánh giá sản phẩm chỉ được thực hiện khi và chỉ khi khách hàng đã đặt hàng và nhận được hàng thành công!");
        }

        // 1. Kiểm tra validation text tối thiểu 50 ký tự theo yêu cầu đề cương
        if (content == null || (content.trim().length() < 50 || content.trim().length() > 1000)) {
            throw new IllegalArgumentException("Bình luận đánh giá sản phẩm phải có từ 50 đến 1.000 ký tự!");
        }

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Số sao đánh giá phải từ 1 đến 5 sao!");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"));

        userRepository.findLockedById(userId).orElseThrow();
        Order order = null;
        if (orderId == null) {
            orderId = getEligibleOrderId(userId, productId);
        }

        if (orderId != null) {
            order = orderRepository.findById(orderId).orElse(null);
            if (order != null) {
                // Kiểm tra đơn hàng đã giao thành công chưa
                if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.COMPLETED && order.getStatus() != OrderStatus.RETURNED) {
                    throw new IllegalStateException("Bạn chỉ có thể đánh giá sản phẩm sau khi đơn hàng đã được giao thành công!");
                }
            }
        }

        if (order == null || !order.getUser().getId().equals(userId)
                || order.getOrderDetails().stream().noneMatch(d -> d.getProduct().getId().equals(productId))) {
            throw new org.springframework.security.access.AccessDeniedException("Đơn đánh giá phải thuộc bạn và chứa sản phẩm này.");
        }
        if (reviewRepository.existsByUserIdAndProductIdAndOrderId(userId, productId, order.getId())) {
            throw new IllegalStateException("Bạn đã đánh giá sản phẩm trong đơn này.");
        }
        // Serialize averages and refresh managed rows so rating writes preserve concurrent price/wallet edits.
        if (order.getPickupStore() != null) {
            Store branch = storeRepository.findLockedById(order.getPickupStore().getId()).orElseThrow();
            entityManager.refresh(branch, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        }
        product = productRepository.findLockedById(productId).orElseThrow();
        entityManager.refresh(product, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        Review review = Review.builder()
                .user(user)
                .product(product)
                .store(order.getPickupStore())
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
        if (order.getPickupStore() != null) {
            Store store = order.getPickupStore();
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

    @Override
    public boolean canReview(Long userId, Long productId) {
        return getEligibleOrderId(userId, productId) != null;
    }

    @Override
    public Long getEligibleOrderId(Long userId, Long productId) {
        if (userId == null || productId == null) return null;
        List<Long> orderIds = orderDetailRepository.findReceivedOrderIdsByProduct(userId, productId);
        return orderIds == null ? null : orderIds.stream().distinct()
                .filter(orderId -> !hasReviewed(userId, productId, orderId)).findFirst().orElse(null);
    }

    @Override
    public boolean canReviewOrder(Long userId, Long productId, Long orderId) {
        if (userId == null || productId == null || orderId == null || hasReviewed(userId, productId, orderId)) return false;
        return orderDetailRepository.findReceivedOrderIdsByProduct(userId, productId).contains(orderId);
    }
}
