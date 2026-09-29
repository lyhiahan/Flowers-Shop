package com.finalproject.repository;

import com.finalproject.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    @org.springframework.data.jpa.repository.Query("select coalesce(sum(i.quantity), 0) from CartItem i where i.cart.user.username = :username")
    Long countUnitsByUsername(@org.springframework.data.repository.query.Param("username") String username);
    List<CartItem> findByCartId(Long cartId);
    Optional<CartItem> findByCartIdAndProductIdAndStoreId(Long cartId, Long productId, Long storeId);
    void deleteByCartId(Long cartId);
}
