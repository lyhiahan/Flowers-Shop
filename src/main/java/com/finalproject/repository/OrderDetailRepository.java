package com.finalproject.repository;

import com.finalproject.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    List<OrderDetail> findByOrderId(Long orderId);

    @Query("SELECT CASE WHEN COUNT(od) > 0 THEN true ELSE false END " +
           "FROM OrderDetail od " +
           "WHERE od.order.user.id = :userId " +
           "AND od.product.id = :productId " +
           "AND od.order.status IN (com.finalproject.enums.OrderStatus.DELIVERED, com.finalproject.enums.OrderStatus.COMPLETED, com.finalproject.enums.OrderStatus.RETURNED)")
    boolean hasPurchasedAndReceived(@Param("userId") Long userId, @Param("productId") Long productId);

    @Query("SELECT od.order.id " +
           "FROM OrderDetail od " +
           "WHERE od.order.user.id = :userId " +
           "AND od.product.id = :productId " +
           "AND od.order.status IN (com.finalproject.enums.OrderStatus.DELIVERED, com.finalproject.enums.OrderStatus.COMPLETED, com.finalproject.enums.OrderStatus.RETURNED) " +
           "ORDER BY od.order.createdAt DESC")
    List<Long> findReceivedOrderIdsByProduct(@Param("userId") Long userId, @Param("productId") Long productId);
}