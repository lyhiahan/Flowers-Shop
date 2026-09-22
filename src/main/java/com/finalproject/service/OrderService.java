package com.finalproject.service;

import com.finalproject.entity.Order;
import com.finalproject.enums.PaymentMethod;

import java.util.List;

public interface OrderService {
    List<Order> checkout(String username, PaymentMethod paymentMethod);
    List<Order> getOrdersByUsername(String username);
    void cancelOrder(String username, Long orderId);
    void scanAndMarkOverdueOrders(); // Cho Scheduler quét định kỳ
}