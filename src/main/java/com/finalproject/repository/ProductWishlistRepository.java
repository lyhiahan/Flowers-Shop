package com.finalproject.repository;

import com.finalproject.entity.ProductWishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductWishlistRepository extends JpaRepository<ProductWishlist, Long> {
    List<ProductWishlist> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<ProductWishlist> findByUserIdAndProductId(Long userId, Long productId);
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    long countByProductId(Long productId);
}
