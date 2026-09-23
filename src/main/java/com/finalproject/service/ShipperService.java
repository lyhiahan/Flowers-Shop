package com.finalproject.service;

import com.finalproject.entity.Order;
import com.finalproject.enums.OrderStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ShipperService {
    List<Order> getAssignedOrders(Long shipperId);
    List<Order> getAssignedOrdersByStatus(Long shipperId, OrderStatus status);
    Order updateDeliveryStatus(Long orderId, Long shipperId, OrderStatus newStatus);
    Map<String, Object> getShipperStats(Long shipperId);
}
