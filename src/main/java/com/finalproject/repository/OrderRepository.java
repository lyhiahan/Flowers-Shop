package com.finalproject.repository;

import com.finalproject.entity.Order;
import com.finalproject.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Order> {
    @Query("select o.user.id from Order o where o.id = :id")
    java.util.Optional<Long> findCustomerIdById(@Param("id") Long id);
    @org.springframework.data.jpa.repository.Modifying
    @Query("""
        update Order o set o.paymentExpiresAt = case when o.paymentMethod = 'MOMO'
            then cast(:momoDeadline as LocalDateTime) else cast(:bankDeadline as LocalDateTime) end
        where o.paymentExpiresAt is null and o.paymentStatus = 'UNPAID'
        and o.paymentMethod in ('MOMO','BANK_TRANSFER')
        and o.status in (com.finalproject.enums.OrderStatus.PENDING,com.finalproject.enums.OrderStatus.CONFIRMED)
        """)
    int initializeLegacyPaymentDeadlines(@Param("momoDeadline") java.time.LocalDateTime momoDeadline,
                                        @Param("bankDeadline") java.time.LocalDateTime bankDeadline);
    @Query("""
        select coalesce(sum(o.totalAmount), 0) from Order o
        where (:storeId is null or o.pickupStore.id = :storeId)
        and (cast(:start as LocalDateTime) is null or o.createdAt >= :start)
        and (cast(:end as LocalDateTime) is null or o.createdAt < :end)
        and o.paymentStatus = 'PAID'
        and (o.status in (com.finalproject.enums.OrderStatus.DELIVERED, com.finalproject.enums.OrderStatus.COMPLETED)
          or (o.status = com.finalproject.enums.OrderStatus.REFUND_REQUESTED
            and exists(select t.id from Transaction t where t.order = o and t.type = 'ORDER_PAYMENT')))
        """)
    java.math.BigDecimal recognizedRevenue(Long storeId, java.time.LocalDateTime start, java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    java.util.Optional<Order> findLockedById(@Param("id") Long id);
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Order> findByPickupStoreIdOrderByCreatedAtDesc(Long storeId);
    List<Order> findByPickupStoreIdAndCreatedAtBetweenOrderByCreatedAtDesc(Long storeId, java.time.LocalDateTime start, java.time.LocalDateTime end);
    List<Order> findByPickupStoreIdAndStatus(Long storeId, OrderStatus status);
    long countByPickupStoreIdAndFundsStatus(Long storeId, com.finalproject.enums.OrderFundsStatus fundsStatus);


    long countByStatus(OrderStatus status);
    List<Order> findTop10ByOrderByCreatedAtDesc();

    // Lọc theo User và trạng thái
    List<Order> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, OrderStatus status);

    @Query("""
        select o from Order o where o.user.id = :userId and
        (o.status = com.finalproject.enums.OrderStatus.CANCELLED or
         (o.status in (com.finalproject.enums.OrderStatus.REFUND_REQUESTED, com.finalproject.enums.OrderStatus.REFUNDED)
          and o.cancellationReason is not null and trim(o.cancellationReason) <> ''))
        order by o.createdAt desc
        """)
    List<Order> findCancelledByUserId(@Param("userId") Long userId);

    boolean existsByLegacyDeliveryContactId(Long userId);

    // Tìm kiếm đơn hàng theo khoảng thời gian để thống kê
    List<Order> findByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
}
