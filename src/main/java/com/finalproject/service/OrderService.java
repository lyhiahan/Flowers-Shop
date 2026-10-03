package com.finalproject.service;

import com.finalproject.entity.Order;
import com.finalproject.enums.PaymentMethod;

import java.util.List;

public interface OrderService {
    List<Order> checkout(String username, List<Long> selectedCartItemIds, PaymentMethod paymentMethod);
    default List<Order> checkout(String username, PaymentMethod paymentMethod) {
        return checkout(username, null, paymentMethod);
    }
    List<Order> checkout(String username, List<Long> selectedCartItemIds, PaymentMethod paymentMethod,
                         String recipientName, String recipientPhone, String recipientAddress, String greetingMessage);
    List<Order> checkout(String username, List<Long> selectedCartItemIds, PaymentMethod paymentMethod,
                         String recipientName, String recipientPhone, String recipientAddress, String greetingMessage, String voucherCode);
    List<Order> checkout(String username, List<Long> selectedCartItemIds, PaymentMethod paymentMethod,
                         String recipientName, String recipientPhone, String recipientAddress, String greetingMessage, String voucherCode,
                         java.time.LocalDate deliveryDate);
    List<Order> checkout(String username, List<Long> selectedCartItemIds, PaymentMethod paymentMethod,
                         String recipientName, String recipientPhone, String recipientAddress, String greetingMessage, String voucherCode,
                         java.time.LocalDate deliveryDate, String shippingMethod, Long pickupStoreId);
    List<Order> getOrdersByUsername(String username);
    List<Order> getOrdersByUsernameAndStatus(String username, com.finalproject.enums.OrderStatus status);
    List<Order> getOrdersByStore(Long storeId);
    List<Order> getAllOrders();
    Order getOrderById(Long orderId);
    void updateOrderStatus(Long orderId, com.finalproject.enums.OrderStatus status);
    void assignShipper(Long orderId, Long shipperId);
    void cancelOrder(String username, Long orderId);
    void requestRefund(String username, Long orderId);
}