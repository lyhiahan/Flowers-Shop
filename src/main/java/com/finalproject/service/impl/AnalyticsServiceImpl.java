package com.finalproject.service.impl;

import com.finalproject.dto.response.ChartSeriesDto;
import com.finalproject.dto.response.RevenueChartDto;
import com.finalproject.entity.Order;
import com.finalproject.enums.OrderStatus;
import com.finalproject.repository.*;
import com.finalproject.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {
    private final OrderRepository orders;
    private final UserRepository users;
    private final StoreRepository stores;
    private final TransactionRepository transactions;

    private List<Order> paidOrders(Long storeId) {
        return (storeId == null ? orders.findAll() : orders.findByPickupStoreIdOrderByCreatedAtDesc(storeId)).stream()
                .filter(o -> "PAID".equals(o.getPaymentStatus()) &&
                        (o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.COMPLETED
                                || (o.getStatus() == OrderStatus.REFUND_REQUESTED && !transactions.findAllByOrderIdAndType(o.getId(), "ORDER_PAYMENT").isEmpty())))
                .toList();
    }

    @Override
    public RevenueChartDto getStoreRevenueStats(Long storeId) {
        BigDecimal total = paidOrders(storeId).stream().map(Order::getTotalAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return RevenueChartDto.builder().totalBuyRevenue(total).totalRentRevenue(BigDecimal.ZERO).totalFineRevenue(BigDecimal.ZERO).build();
    }

    @Override
    public ChartSeriesDto getRevenueChart(String timeframe, Long storeId) {
        List<Point> points = paidOrders(storeId).stream().filter(o -> o.getCreatedAt() != null)
                .map(o -> new Point(o.getCreatedAt(), o.getTotalAmount())).toList();
        return chart(timeframe, points);
    }

    @Override
    public ChartSeriesDto getNewUsersChart(String timeframe) {
        // Legacy user records store a date only, so never invent hourly registration times.
        return chart("hour".equals(timeframe) ? "day" : timeframe, users.findAll().stream()
                .filter(u -> u.getCreatedDate() != null).map(u -> new Point(u.getCreatedDate().atStartOfDay(), BigDecimal.ONE)).toList());
    }

    @Override
    public ChartSeriesDto getNewStoresChart(String timeframe) {
        return chart(timeframe, stores.findAll().stream().filter(s -> s.getCreatedAt() != null)
                .map(s -> new Point(s.getCreatedAt(), BigDecimal.ONE)).toList());
    }

    @Override
    public ChartSeriesDto getProductRevenueChart(Long storeId) {
        Map<Long, BigDecimal> totals = new HashMap<>();
        Map<Long, String> names = new HashMap<>();
        for (Order order : paidOrders(storeId)) {
            BigDecimal gross = order.getOrderDetails().stream().map(d -> d.getUnitPrice().multiply(BigDecimal.valueOf(d.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal remaining = order.getTotalAmount();
            var details = order.getOrderDetails();
            for (int i = 0; i < details.size(); i++) {
                var detail = details.get(i);
                BigDecimal amount = i == details.size() - 1 ? remaining : gross.signum() == 0 ? BigDecimal.ZERO :
                        detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getQuantity())).multiply(order.getTotalAmount())
                                .divide(gross, 2, RoundingMode.HALF_UP);
                remaining = remaining.subtract(amount);
                Long productId = detail.getProduct().getId();
                names.put(productId, detail.getProduct().getName());
                totals.merge(productId, amount, BigDecimal::add);
            }
        }
        var sorted = totals.entrySet().stream().sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed()).limit(7).toList();
        var values = sorted.stream().map(Map.Entry::getValue).toList();
        return ChartSeriesDto.builder().timeframe("product").labels(sorted.stream().map(e -> names.get(e.getKey())).toList())
                .data(values).total(values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)).build();
    }

    private record Point(LocalDateTime time, BigDecimal value) {}

    private ChartSeriesDto chart(String requested, List<Point> points) {
        String timeframe = Set.of("hour", "day", "month", "year").contains(requested == null ? "" : requested) ? requested : "day";
        LocalDate today = LocalDate.now();
        int count = switch (timeframe) { case "hour" -> 24; case "month" -> 12; case "year" -> 5; default -> 7; };
        List<String> labels = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            LocalDateTime start;
            LocalDateTime end;
            String label;
            switch (timeframe) {
                case "hour" -> { start = today.atStartOfDay().plusHours(i); end = start.plusHours(1); label = i + "h"; }
                case "month" -> { start = LocalDate.of(today.getYear(), i + 1, 1).atStartOfDay(); end = start.plusMonths(1); label = "T" + (i + 1); }
                case "year" -> { start = LocalDate.of(today.getYear() - 4 + i, 1, 1).atStartOfDay(); end = start.plusYears(1); label = String.valueOf(start.getYear()); }
                default -> { start = today.minusDays(6 - i).atStartOfDay(); end = start.plusDays(1); label = start.format(DateTimeFormatter.ofPattern("dd/MM")); }
            }
            labels.add(label);
            values.add(points.stream().filter(p -> !p.time().isBefore(start) && p.time().isBefore(end))
                    .map(Point::value).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
        }
        return ChartSeriesDto.builder().timeframe(timeframe).labels(labels).data(values)
                .total(values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)).build();
    }
}
