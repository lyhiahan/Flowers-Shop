package com.finalproject.repository;

import com.finalproject.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCartId(Long cartId);
    Optional<CartItem> findByCartIdAndProductIdAndStoreId(Long cartId, Long productId, Long storeId);
    void deleteByCartId(Long cartId);
}