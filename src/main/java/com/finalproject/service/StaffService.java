package com.finalproject.service;

import com.finalproject.entity.Order;
import java.math.BigDecimal;
import java.util.List;
import com.finalproject.enums.OrderStatus;
import org.springframework.data.domain.Page;

public interface StaffService {
    List<Order> getOrdersByStaffStore(String staffUsername);
    Page<Order> searchOrders(String username, String section, String keyword, OrderStatus status,
                            String fulfillment, int page, int size);
    Order getOrder(String username, Long id);
    List<OrderAction> actions(Order order);
    void updateStatus(String username, Long id, OrderStatus next, boolean codCollected);
    record OrderAction(OrderStatus status, String label, boolean collectCod, BigDecimal amount) {}
    void approveOrder(String staffUsername, Long orderId);
    void deliverOrder(String staffUsername, Long orderId);
    void processReturn(String staffUsername, Long orderId, Long returnStoreId, boolean waiveFine, BigDecimal damageFee);
    void convertRentToBuy(String staffUsername, Long orderId);
}
