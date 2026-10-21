package com.finalproject.repository;

import com.finalproject.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /** Tổng số lượng toàn giỏ của user (dùng để cập nhật biểu tượng giỏ) */
    @org.springframework.data.jpa.repository.Query("select coalesce(sum(i.quantity), 0) from CartItem i where i.cart.user.username = :username")
    Long countUnitsByUsername(@org.springframework.data.repository.query.Param("username") String username);

    /** Tổng số lượng toàn giỏ theo cartId (không cần load user) */
    @org.springframework.data.jpa.repository.Query("select coalesce(sum(i.quantity), 0) from CartItem i where i.cart.id = :cartId")
    Long sumQuantityByCartId(@org.springframework.data.repository.query.Param("cartId") Long cartId);

    /** Tổng SL đã có trong giỏ cho một sản phẩm+chi nhánh (không load quan hệ) */
    @org.springframework.data.jpa.repository.Query(
        "select coalesce(sum(i.quantity), 0) from CartItem i " +
        "where i.cart.id = :cartId and i.product.id = :productId and i.store.id = :storeId")
    Long sumQtyByCartProductStore(
        @org.springframework.data.repository.query.Param("cartId") Long cartId,
        @org.springframework.data.repository.query.Param("productId") Long productId,
        @org.springframework.data.repository.query.Param("storeId") Long storeId);

    /** Cart lines are identified by product and branch, not a tentative delivery date. */
    @org.springframework.data.jpa.repository.Query(
        "select i from CartItem i " +
        "where i.cart.id = :cartId and i.product.id = :productId and i.store.id = :storeId " +
        "and i.orderType = :orderType order by i.id")
    List<CartItem> findMatchingLines(
        @org.springframework.data.repository.query.Param("cartId") Long cartId,
        @org.springframework.data.repository.query.Param("productId") Long productId,
        @org.springframework.data.repository.query.Param("storeId") Long storeId,
        @org.springframework.data.repository.query.Param("orderType") com.finalproject.enums.OrderType orderType);

    List<CartItem> findByCartId(Long cartId);
    Optional<CartItem> findByCartIdAndProductIdAndStoreId(Long cartId, Long productId, Long storeId);
    void deleteByCartId(Long cartId);
}
