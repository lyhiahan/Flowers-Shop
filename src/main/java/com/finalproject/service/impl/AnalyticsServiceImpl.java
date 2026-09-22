package com.finalproject.service.impl;

import com.finalproject.dto.response.RevenueChartDto;
import com.finalproject.entity.Order;
import com.finalproject.enums.OrderStatus;
import com.finalproject.enums.OrderType;
import com.finalproject.repository.OrderRepository;
import com.finalproject.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final OrderRepository orderRepository;

    @Override
    public RevenueChartDto getStoreRevenueStats(Long storeId) {
        List<Order> orders = orderRepository.findByPickupStoreIdOrderByCreatedAtDesc(storeId);

        BigDecimal buyRev = BigDecimal.ZERO;
        BigDecimal rentRev = BigDecimal.ZERO;
        BigDecimal fineRev = BigDecimal.ZERO;

        for (Order order : orders) {
            // Chỉ tính doanh thu các đơn không bị CANCELLED hoặc PENDING
            if (order.getStatus() != OrderStatus.CANCELLED && order.getStatus() != OrderStatus.PENDING) {
                if (order.getOrderType() == OrderType.BUY) {
                    buyRev = buyRev.add(order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO);
                } else if (order.getOrderType() == OrderType.RENT) {
                    rentRev = rentRev.add(order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO);
                    fineRev = fineRev.add(order.getFineAmount() != null ? order.getFineAmount() : BigDecimal.ZERO);
                }
            }
        }

        return RevenueChartDto.builder()
                .totalBuyRevenue(buyRev)
                .totalRentRevenue(rentRev)
                .totalFineRevenue(fineRev)
                .build();
    }
}