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
    @Query("""
        select count(distinct d.order.id) from OrderDetail d where d.product.id=:productId
        and d.order.status in (com.finalproject.enums.OrderStatus.PENDING, com.finalproject.enums.OrderStatus.CONFIRMED,
            com.finalproject.enums.OrderStatus.PREPARING, com.finalproject.enums.OrderStatus.PICKED_UP,
            com.finalproject.enums.OrderStatus.DELIVERING, com.finalproject.enums.OrderStatus.RENTING,
            com.finalproject.enums.OrderStatus.OVERDUE)
        """)
    long countOpenOrders(@Param("productId") Long productId);

    default boolean hasOpenOrders(Long productId) {
        return countOpenOrders(productId) > 0;
    }

    @Query("""
        select d.product.id, count(distinct d.order.id) from OrderDetail d
        where d.order.status in (com.finalproject.enums.OrderStatus.PENDING, com.finalproject.enums.OrderStatus.CONFIRMED,
            com.finalproject.enums.OrderStatus.PREPARING, com.finalproject.enums.OrderStatus.PICKED_UP,
            com.finalproject.enums.OrderStatus.DELIVERING, com.finalproject.enums.OrderStatus.RENTING,
            com.finalproject.enums.OrderStatus.OVERDUE)
        group by d.product.id
        """)
    List<Object[]> countOpenOrdersByProduct();

    @Query("""
        select count(d) > 0 from OrderDetail d where d.product.id=:productId and d.order.pickupStore.id=:storeId
        and d.order.status in (com.finalproject.enums.OrderStatus.PENDING, com.finalproject.enums.OrderStatus.CONFIRMED,
            com.finalproject.enums.OrderStatus.PREPARING, com.finalproject.enums.OrderStatus.PICKED_UP,
            com.finalproject.enums.OrderStatus.DELIVERING,
            com.finalproject.enums.OrderStatus.RENTING, com.finalproject.enums.OrderStatus.OVERDUE)
        """)
    boolean hasOpenOrdersAtStore(Long productId, Long storeId);
    interface SalesRank {
        Long getId();
        String getName();
        Long getSold();
    }

    @Query("""
        select d.product.id as id, d.product.name as name, sum(d.quantity) as sold
        from OrderDetail d
        where (:storeId is null or d.order.pickupStore.id = :storeId)
        and d.order.createdAt >= :start and d.order.createdAt < :end
        and d.order.paymentStatus = 'PAID'
        and (d.order.status in (com.finalproject.enums.OrderStatus.DELIVERED, com.finalproject.enums.OrderStatus.COMPLETED)
          or (d.order.status = com.finalproject.enums.OrderStatus.REFUND_REQUESTED
            and exists(select t.id from Transaction t where t.order.id = d.order.id and t.type = 'ORDER_PAYMENT')))
        group by d.product.id, d.product.name
        order by sum(d.quantity) desc, d.product.id desc
        """)
    List<SalesRank> rankSales(Long storeId, LocalDateTime start, LocalDateTime end, Pageable pageable);

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
