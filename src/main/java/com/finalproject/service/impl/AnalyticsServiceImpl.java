package com.finalproject.service.impl;

import com.finalproject.dto.response.ChartSeriesDto;
import com.finalproject.dto.response.RevenueChartDto;
import com.finalproject.entity.Order;
import com.finalproject.enums.OrderStatus;
import com.finalproject.repository.*;
import com.finalproject.service.AnalyticsService;
import com.finalproject.service.ReportRange;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
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

    private boolean recognized(Order order) {
        return "PAID".equals(order.getPaymentStatus()) && (order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.COMPLETED || (order.getStatus() == OrderStatus.REFUND_REQUESTED
                && !transactions.findAllByOrderIdAndType(order.getId(), "ORDER_PAYMENT").isEmpty()));
    }

    private Specification<Order> orderScope(Long storeId, ReportRange range) {
        return (root, query, cb) -> cb.and(
                storeId == null ? cb.conjunction() : cb.equal(root.get("pickupStore").get("id"), storeId),
                range == null ? cb.conjunction() : cb.and(cb.greaterThanOrEqualTo(root.get("createdAt"), range.start()),
                        cb.lessThan(root.get("createdAt"), range.endExclusive())));
    }

    @Override
    public Map<String, Object> dashboard(Long storeId, String timeframe, String criteria) {
        return dashboard(storeId, ReportRange.resolve(timeframe, null, null, clock), criteria);
    }

    @Override
    public Map<String, Object> dashboard(Long storeId, ReportRange range, String criteria) {
        if (!Set.of("revenue", "orders", "products").contains(criteria))
            throw new IllegalArgumentException("Tiêu chí thống kê không hợp lệ.");
        var scope = orderScope(storeId, range);
        var sort = Sort.by(Sort.Direction.DESC, "createdAt", "id");
        var matching = orders.findAll(scope, sort);
        var revenue = chart(range, matching.stream().filter(this::recognized)
                .map(o -> new Point(o.getCreatedAt(), o.getTotalAmount())).toList());
        var orderChart = chart(range, matching.stream().map(o -> new Point(o.getCreatedAt(), BigDecimal.ONE)).toList());
        var quantityChart = chart(range, matching.stream().filter(this::recognized)
                .map(o -> new Point(o.getCreatedAt(), BigDecimal.valueOf((o.getOrderDetails() == null
                        ? List.<com.finalproject.entity.OrderDetail>of() : o.getOrderDetails()).stream()
                        .mapToLong(d -> d.getQuantity() == null ? 0 : d.getQuantity()).sum()))).toList());
        long newProducts = products.count((root, query, cb) -> cb.and(
                cb.or(cb.isFalse(root.get("isDeleted")), cb.isNull(root.get("isDeleted"))),
                storeId == null ? cb.conjunction() : cb.equal(root.get("store").get("id"), storeId),
                cb.greaterThanOrEqualTo(root.get("createdAt"), range.start()), cb.lessThan(root.get("createdAt"), range.endExclusive())));
        var result = new HashMap<String, Object>();
        result.put("reportRange", range);
        result.put("totalProductsCount", newProducts);
        result.put("totalOrdersCount", (long) matching.size());
        result.put("chartLabels", revenue.getLabels()); result.put("chartRevenueValues", revenue.getData());
        result.put("chartOrderValues", orderChart.getData()); result.put("chartProductValues", quantityChart.getData());
        result.put("chartValues", "orders".equals(criteria) ? orderChart.getData() : "products".equals(criteria) ? quantityChart.getData() : revenue.getData());
        result.put("chartUnit", "orders".equals(criteria) ? "Đơn hàng" : "products".equals(criteria) ? "Số sản phẩm bán" : "Doanh thu (VNĐ)");
        result.put("flowerRevenue", revenue.getTotal());
        result.put("totalRevenue", revenue.getTotal());
        result.put("topProducts", details.rankSales(storeId, range.start(), range.endExclusive(), PageRequest.of(0, 1000)));
        result.put("recentOrders", orders.findAll(scope, PageRequest.of(0, 1000, sort)).getContent());
        result.put("sampleCount", matching.size()); result.put("rangeOrdersCount", (long) matching.size());
        result.put("hasRevenueData", !matching.isEmpty()); result.put("dataLoadError", false);
        return result;
    }

    @Override
    public RevenueChartDto getStoreRevenueStats(Long storeId) {
        return RevenueChartDto.builder().totalBuyRevenue(orders.recognizedRevenue(storeId, null, null))
                .totalRentRevenue(BigDecimal.ZERO).totalFineRevenue(BigDecimal.ZERO).build();
    }

    @Override
    public ChartSeriesDto getRevenueChart(String timeframe, Long storeId) {
        return getRevenueChart(ReportRange.resolve(timeframe, null, null, clock), storeId);
    }

    @Override
    public ChartSeriesDto getRevenueChart(ReportRange range, Long storeId) {
        return chart(range, orders.findAll(orderScope(storeId, range)).stream().filter(this::recognized)
                .map(o -> new Point(o.getCreatedAt(), o.getTotalAmount())).toList());
    }

    @Override
    public ChartSeriesDto getNewUsersChart(String timeframe) {
        return getNewUsersChart(ReportRange.resolve(timeframe, null, null, clock));
    }

    @Override
    public ChartSeriesDto getNewUsersChart(ReportRange range) {
        var matching = users.findAll((root, query, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("createdAt"), range.start()), cb.lessThan(root.get("createdAt"), range.endExclusive()),
                range.bucket() == ReportRange.Bucket.HOUR ? cb.isTrue(root.get("creationTimeKnown")) : cb.conjunction()));
        return chart(range, matching.stream().map(u -> new Point(u.getCreatedAt(), BigDecimal.ONE)).toList());
    }

    @Override
    public ChartSeriesDto getNewStoresChart(String timeframe) {
        return getNewStoresChart(ReportRange.resolve(timeframe, null, null, clock));
    }

    @Override
    public ChartSeriesDto getNewStoresChart(ReportRange range) {
        var matching = stores.findAll((root, query, cb) -> cb.and(cb.greaterThanOrEqualTo(root.get("createdAt"), range.start()),
                cb.lessThan(root.get("createdAt"), range.endExclusive())));
        return chart(range, matching.stream().map(s -> new Point(s.getCreatedAt(), BigDecimal.ONE)).toList());
    }

    @Override
    public ChartSeriesDto getProductRevenueChart(Long storeId) { return getProductRevenueChart(storeId, null); }

    @Override
    public ChartSeriesDto getProductRevenueChart(Long storeId, ReportRange range) {
        Map<Long, BigDecimal> totals = new HashMap<>();
        Map<Long, String> names = new HashMap<>();
        for (Order order : orders.findAll(orderScope(storeId, range)).stream().filter(this::recognized).toList()) {
            var lines = order.getOrderDetails() == null ? List.<com.finalproject.entity.OrderDetail>of() : order.getOrderDetails();
            BigDecimal gross = lines.stream().map(d -> d.getUnitPrice().multiply(BigDecimal.valueOf(d.getQuantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal remaining = order.getTotalAmount();
            for (int i = 0; i < lines.size(); i++) {
                var detail = lines.get(i);
                BigDecimal amount = i == lines.size() - 1 ? remaining : gross.signum() == 0 ? BigDecimal.ZERO
                        : detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getQuantity())).multiply(order.getTotalAmount()).divide(gross, 2, RoundingMode.HALF_UP);
                remaining = remaining.subtract(amount);
                Long productId = detail.getProduct().getId();
                names.put(productId, detail.getProduct().getName());
                totals.merge(productId, amount, BigDecimal::add);
            }
        }
        var sorted = totals.entrySet().stream().sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed()).toList();
        List<String> labels = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();
        for (var entry : sorted.stream().limit(7).toList()) { labels.add(names.get(entry.getKey())); values.add(entry.getValue()); }
        if (sorted.size() > 7) {
            labels.add("Các sản phẩm khác");
            values.add(sorted.stream().skip(7).map(Map.Entry::getValue).reduce(BigDecimal.ZERO, BigDecimal::add));
        }
        return ChartSeriesDto.builder().timeframe("product").labels(labels).data(values)
                .total(values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)).build();
    }

    private record Point(LocalDateTime time, BigDecimal value) {}

    private ChartSeriesDto chart(ReportRange range, List<Point> points) {
        Map<LocalDateTime, BigDecimal> totals = new LinkedHashMap<>();
        range.keys().forEach(key -> totals.put(key, BigDecimal.ZERO));
        for (Point point : points) {
            if (range.contains(point.time()) && point.value() != null)
                totals.computeIfPresent(range.key(point.time()), (key, total) -> total.add(point.value()));
        }
        var values = new ArrayList<>(totals.values());
        return ChartSeriesDto.builder().timeframe(range.period()).labels(totals.keySet().stream().map(range::bucketLabel).toList())
                .data(values).total(values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)).build();
    }
}
