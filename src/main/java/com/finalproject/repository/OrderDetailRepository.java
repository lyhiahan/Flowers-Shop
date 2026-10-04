package com.finalproject.repository;

import com.finalproject.entity.OrderDetail;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    // Lấy ID sản phẩm bán chạy nhất trong đơn hoàn thành kể từ mốc thời gian (loại trừ đơn hủy/hoàn/trả)
    @Query("SELECT od.product.id " +
           "FROM OrderDetail od " +
           "WHERE od.order.status IN (com.finalproject.enums.OrderStatus.DELIVERED, com.finalproject.enums.OrderStatus.COMPLETED) " +
           "AND od.order.createdAt >= :since " +
           "GROUP BY od.product.id " +
           "ORDER BY SUM(od.quantity) DESC")
    List<Long> findTopSellingProductIdsSince(@Param("since") LocalDateTime since, Pageable pageable);

    // Tính tổng số lượng đang giữ (Reserved) cho các đơn hàng đang xử lý theo sản phẩm và chi nhánh
    @Query("SELECT od.product.id, od.order.pickupStore.id, SUM(od.quantity) " +
           "FROM OrderDetail od " +
           "WHERE od.order.status IN (com.finalproject.enums.OrderStatus.PENDING, com.finalproject.enums.OrderStatus.CONFIRMED, com.finalproject.enums.OrderStatus.PREPARING) " +
           "AND od.order.pickupStore IS NOT NULL " +
           "GROUP BY od.product.id, od.order.pickupStore.id")
    List<Object[]> findReservedStockGrouped();
}