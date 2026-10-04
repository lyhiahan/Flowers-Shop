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
    private final ProductRepository products;
    private final Clock clock;
    private final OrderDetailRepository details;

    private boolean recognized(Order o) {
        return "PAID".equals(o.getPaymentStatus()) && (o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.COMPLETED
                || (o.getStatus() == OrderStatus.REFUND_REQUESTED && !transactions.findAllByOrderIdAndType(o.getId(), "ORDER_PAYMENT").isEmpty()));
    }

    @Override
    public Map<String, Object> dashboard(Long storeId, String timeframe, String criteria) {
        if (!Set.of("hour", "day", "month", "year").contains(timeframe) || !Set.of("revenue", "orders", "products").contains(criteria)) throw new IllegalArgumentException("Bộ lọc thống kê không hợp lệ.");
        var today = LocalDate.now(clock);
        LocalDateTime start = switch (timeframe) {
            case "hour" -> today.atStartOfDay();
            case "month" -> LocalDate.of(today.getYear(), 1, 1).atStartOfDay();
            case "year" -> LocalDate.of(today.getYear()-4, 1, 1).atStartOfDay();
            default -> today.minusDays(6).atStartOfDay();
        };
        var end = rangeEnd(timeframe);
        org.springframework.data.jpa.domain.Specification<Order> scope = (root, query, cb) -> storeId == null ? cb.conjunction() : cb.equal(root.get("pickupStore").get("id"), storeId);
        var range = scope.and((root, query, cb) -> cb.and(cb.greaterThanOrEqualTo(root.get("createdAt"), start), cb.lessThan(root.get("createdAt"), end)));
        var sorted = org.springframework.data.domain.PageRequest.of(0, 1000, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt", "id"));
        var sampled = orders.findAll(range, sorted);
        var latest = orders.findAll(scope, sorted).getContent();
        var orderPoints = sampled.stream().map(o -> new Point(o.getCreatedAt(), BigDecimal.ONE)).toList();
        var revenuePoints = sampled.stream().filter(this::recognized).map(o -> new Point(o.getCreatedAt(), o.getTotalAmount())).toList();
        var quantityPoints = sampled.stream().filter(this::recognized).map(o -> new Point(o.getCreatedAt(), BigDecimal.valueOf((o.getOrderDetails() == null ? java.util.List.<com.finalproject.entity.OrderDetail>of() : o.getOrderDetails()).stream().mapToLong(d -> d.getQuantity() == null ? 0 : d.getQuantity()).sum()))).toList();
        var revenue = chart(timeframe, revenuePoints); var orderChart = chart(timeframe, orderPoints); var quantityChart = chart(timeframe, quantityPoints);
        var scopeProducts = products.findAll((root, query, cb) -> cb.and(cb.isFalse(root.get("isDeleted")), storeId == null ? cb.conjunction() : cb.equal(root.get("store").get("id"), storeId)),
                org.springframework.data.domain.PageRequest.of(0, 1000, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "sold", "id")));
        var result = new HashMap<String, Object>();
        result.put("totalProductsCount", scopeProducts.getTotalElements()); result.put("totalOrdersCount", orders.count(scope));
        result.put("chartLabels", revenue.getLabels()); result.put("chartRevenueValues", revenue.getData());
        result.put("chartOrderValues", orderChart.getData()); result.put("chartProductValues", quantityChart.getData());
        result.put("chartValues", "orders".equals(criteria) ? orderChart.getData() : "products".equals(criteria) ? quantityChart.getData() : revenue.getData());
        result.put("chartUnit", "orders".equals(criteria) ? "Đơn hàng" : "products".equals(criteria) ? "Số sản phẩm bán" : "Doanh thu (VNĐ)");
        result.put("flowerRevenue", orders.recognizedRevenue(storeId, start, end));
        result.put("topProducts", details.rankSales(storeId, start, end, org.springframework.data.domain.PageRequest.of(0, 1000))); result.put("recentOrders", latest);
        result.put("sampleCount", sampled.getNumberOfElements()); result.put("rangeOrdersCount", sampled.getTotalElements());
        result.put("hasRevenueData", sampled.hasContent()); result.put("dataLoadError", false);
        return result;
    }


    private List<Order> paidOrders(Long storeId) {
        return (storeId == null ? orders.findAll() : orders.findByPickupStoreIdOrderByCreatedAtDesc(storeId)).stream()
                .filter(o -> "PAID".equals(o.getPaymentStatus()) &&
                        (o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.COMPLETED
                                || (o.getStatus() == OrderStatus.REFUND_REQUESTED && !transactions.findAllByOrderIdAndType(o.getId(), "ORDER_PAYMENT").isEmpty())))
                .toList();
    }

    @Override
    public RevenueChartDto getStoreRevenueStats(Long storeId) {
        BigDecimal total = orders.recognizedRevenue(storeId, null, null);
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
        var newest = users.findAll((root, query, cb) -> cb.and(cb.greaterThanOrEqualTo(root.get("createdAt"), rangeStart(timeframe)), cb.lessThan(root.get("createdAt"), rangeEnd(timeframe)), "hour".equals(timeframe) ? cb.isTrue(root.get("creationTimeKnown")) : cb.conjunction()), org.springframework.data.domain.PageRequest.of(0, 1000, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt", "id")));
        return chart(timeframe, newest.stream().filter(u -> u.getCreatedAt() != null)
                .filter(u -> !"hour".equals(timeframe) || Boolean.TRUE.equals(u.getCreationTimeKnown()))
                .map(u -> new Point(u.getCreatedAt(), BigDecimal.ONE)).toList());
    }

    @Override
    public ChartSeriesDto getNewStoresChart(String timeframe) {
        return chart(timeframe, stores.findAll((root, query, cb) -> cb.and(cb.greaterThanOrEqualTo(root.get("createdAt"), rangeStart(timeframe)), cb.lessThan(root.get("createdAt"), rangeEnd(timeframe))), org.springframework.data.domain.PageRequest.of(0, 1000,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt", "id"))).stream()
                .filter(s -> s.getCreatedAt() != null).map(s -> new Point(s.getCreatedAt(), BigDecimal.ONE)).toList());
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

    private LocalDateTime rangeStart(String timeframe) {
        var today = LocalDate.now(clock);
        return switch (timeframe == null ? "day" : timeframe) {
            case "hour" -> today.atStartOfDay();
            case "month" -> LocalDate.of(today.getYear(), 1, 1).atStartOfDay();
            case "year" -> LocalDate.of(today.getYear() - 4, 1, 1).atStartOfDay();
            default -> today.minusDays(6).atStartOfDay();
        };
    }
    private LocalDateTime rangeEnd(String timeframe) {
        var today = LocalDate.now(clock);
        return ("month".equals(timeframe) || "year".equals(timeframe))
                ? LocalDate.of(today.getYear() + 1, 1, 1).atStartOfDay() : today.plusDays(1).atStartOfDay();
    }

    private record Point(LocalDateTime time, BigDecimal value) {}

    private ChartSeriesDto chart(String requested, List<Point> points) {
        String timeframe = Set.of("hour", "day", "month", "year").contains(requested == null ? "" : requested) ? requested : "day";
        LocalDate today = LocalDate.now(clock);
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
