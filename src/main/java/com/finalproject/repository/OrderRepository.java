package com.finalproject.repository;

import com.finalproject.entity.Order;
import com.finalproject.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Order> findByPickupStoreIdOrderByCreatedAtDesc(Long storeId);
    List<Order> findByPickupStoreIdAndStatus(Long storeId, OrderStatus status);

    // Tìm các đơn hàng thuê quá hạn (RENTING mà có sản phẩm hết hạn trước ngày hiện tại)
    @Query("SELECT DISTINCT o FROM Order o JOIN o.orderDetails od " +
           "WHERE o.status = :status AND od.rentEndDate < :today")
    List<Order> findOverdueOrders(@Param("status") OrderStatus status, @Param("today") LocalDate today);
}