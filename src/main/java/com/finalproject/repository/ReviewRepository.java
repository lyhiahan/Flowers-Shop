package com.finalproject.repository;

import com.finalproject.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);
    List<Review> findByStoreIdOrderByCreatedAtDesc(Long storeId);
    boolean existsByUserIdAndProductIdAndOrderId(Long userId, Long productId, Long orderId);
}
