package com.finalproject.service.impl;

import com.finalproject.entity.Order;
import com.finalproject.enums.OrderStatus;
import com.finalproject.repository.OrderRepository;
import com.finalproject.service.ShipperService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShipperServiceImpl implements ShipperService {

    private final OrderRepository orderRepository;

    @Override
    public List<Order> getAssignedOrders(Long shipperId) {
        return orderRepository.findByShipperIdOrderByCreatedAtDesc(shipperId);
    }

    @Override
    public List<Order> getAssignedOrdersByStatus(Long shipperId, OrderStatus status) {
        return orderRepository.findByShipperIdAndStatus(shipperId, status);
    }

    @Override
    @Transactional
    public Order updateDeliveryStatus(Long orderId, Long shipperId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + orderId));

        if (order.getShipper() == null || !order.getShipper().getId().equals(shipperId)) {
            throw new IllegalArgumentException("Đơn hàng này không thuộc phân công của bạn!");
        }

        order.setStatus(newStatus);
        if (newStatus == OrderStatus.DELIVERED) {
            order.setPaymentStatus("PAID");
        }

        return orderRepository.save(order);
    }

    @Override
    public Map<String, Object> getShipperStats(Long shipperId) {
        Map<String, Object> stats = new HashMap<>();
        List<Order> allAssigned = orderRepository.findByShipperIdOrderByCreatedAtDesc(shipperId);

        long total = allAssigned.size();
        long delivered = allAssigned.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.COMPLETED).count();
        long delivering = allAssigned.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERING).count();
        long waiting = allAssigned.stream().filter(o -> o.getStatus() == OrderStatus.PICKED_UP || o.getStatus() == OrderStatus.CONFIRMED).count();

        BigDecimal totalCod = allAssigned.stream()
                .filter(o -> (o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.COMPLETED) && "COD".equalsIgnoreCase(o.getPaymentMethod()))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        stats.put("total", total);
        stats.put("delivered", delivered);
        stats.put("deliveredCount", delivered);
        stats.put("delivering", delivering);
        stats.put("deliveringCount", delivering);
        stats.put("waiting", waiting);
        stats.put("waitingCount", waiting);
        stats.put("totalCod", totalCod);
        stats.put("totalCollectedCod", totalCod != null ? totalCod : BigDecimal.ZERO);

        return stats;
    }
}
