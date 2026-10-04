package com.finalproject.controller.manager;

import com.finalproject.entity.*;
import com.finalproject.enums.OrderStatus;
import com.finalproject.repository.*;
import com.finalproject.service.StoreAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controller triển khai Use Case UC_024: Thống kê doanh thu (Revenue statistics)
 */
@Controller
@RequestMapping({"/manager", "/vendor"})
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class ManagerDashboardController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ManagerDashboardController.class);

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final StoreInventoryRepository storeInventoryRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final StoreAccessService storeAccess;

    public record TopFlowerDto(
            Long id,
            String name,
            String imageUrl,
            String categoryName,
            BigDecimal price,
            int quantitySold,
            BigDecimal revenue,
            Double rating
    ) {}

    /**
     * Main Flow UC_024:
     * Bước 5: Chuyển đến trang vendor dashboard của cửa hàng tương ứng
     * Bước 6: Hiển thị tổng số sản phẩm, đơn hàng
     * Bước 7: Tùy chọn xem thống kê theo đơn hàng, sản phẩm, thời gian (giờ, ngày, tháng, năm), loại biểu đồ (đường, cột, tròn)
     * Bước 8: Hiển thị bên cạnh biểu đồ là Top sản phẩm bán chạy, Top đơn hàng mới nhất
     */
    @GetMapping({"", "/dashboard"})
    public String showManagerDashboard(
            @RequestParam(name = "storeId", required = false) Long storeIdParam,
            @RequestParam(name = "timeRange", defaultValue = "day") String timeRange,
            @RequestParam(name = "criteria", defaultValue = "revenue") String criteria,
            @RequestParam(name = "chartType", defaultValue = "line") String chartType,
            @RequestParam(name = "startDate", required = false) String startDateStr,
            @RequestParam(name = "endDate", required = false) String endDateStr,
            Principal principal,
            Model model) {

        User manager = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new AccessDeniedException("Người dùng không tồn tại"));

        // Danh sách cửa hàng mà Vendor/Manager được phân quyền (theo UC_018)
        List<Store> accessibleStores;
        if (manager.getRole() == com.finalproject.enums.Role.ADMIN) {
            accessibleStores = storeRepository.findAll();
        } else {
            Set<Long> storeIds = new LinkedHashSet<>();
            List<Store> owned = storeRepository.findByOwnerId(manager.getId());
            for (Store s : owned) storeIds.add(s.getId());
            if (manager.getStore() != null) storeIds.add(manager.getStore().getId());
            accessibleStores = storeRepository.findAllById(storeIds);
        }

        Store store = null;
        if (storeIdParam != null) {
            for (Store s : accessibleStores) {
                if (s.getId().equals(storeIdParam)) {
                    store = s;
                    manager.setStore(s);
                    userRepository.save(manager);
                    break;
                }
            }
        }
        if (store == null) {
            store = resolveStore(manager);
        }
        if (store == null && !accessibleStores.isEmpty()) {
            store = accessibleStores.get(0);
            manager.setStore(store);
            userRepository.save(manager);
        }
        if (store == null) {
            return "redirect:/manager/stores";
        }
        storeAccess.requireStore(store);

        Long storeId = store.getId();

        // Kiểm tra quyền quan hệ theo UC_018: Sở hữu vs Hợp tác
        boolean isOwned = store.getOwner() != null && store.getOwner().getId().equals(manager.getId());
        String storeRelationType = isOwned ? "Sở hữu" : "Hợp tác";

        // Tên hiển thị cửa hàng
        String storeShortTitle = store.getName() != null ? store.getName() : "Cửa hàng hoa tươi";

        model.addAttribute("store", store);
        model.addAttribute("accessibleStores", accessibleStores);
        model.addAttribute("storeRelationType", storeRelationType);
        model.addAttribute("isOwned", isOwned);
        model.addAttribute("storeShortTitle", storeShortTitle);
        model.addAttribute("lastUpdatedTime", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));

        // Chuẩn hóa tham số
        String normalizedTime = timeRange.toLowerCase();
        if ("7days".equals(normalizedTime)) normalizedTime = "day";
        if ("today".equals(normalizedTime)) normalizedTime = "hour";

        String normalizedCriteria = criteria.toLowerCase();
        if (!"orders".equals(normalizedCriteria) && !"products".equals(normalizedCriteria)) {
            normalizedCriteria = "revenue";
        }

        String normalizedChartType = chartType.toLowerCase();
        if (!"bar".equals(normalizedChartType) && !"pie".equals(normalizedChartType)) {
            normalizedChartType = "line";
        }

        // Bước 6: Hiển thị tổng số sản phẩm, đơn hàng
        List<Order> allCurrentStoreOrders;
        try {
            allCurrentStoreOrders = orderRepository.findByPickupStoreIdOrderByCreatedAtDesc(storeId);
        } catch (Exception e) {
            allCurrentStoreOrders = Collections.emptyList();
        }

        List<Product> allStoreProducts;
        try {
            allStoreProducts = productRepository.findByStoreIdOrderByCreatedAtDesc(storeId);
        } catch (Exception e) {
            allStoreProducts = Collections.emptyList();
        }

        long totalOrdersCount = allCurrentStoreOrders.size();
        long totalProductsCount = allStoreProducts.size();

        model.addAttribute("totalOrdersCount", totalOrdersCount);
        model.addAttribute("totalProductsCount", totalProductsCount);
        model.addAttribute("timeRange", normalizedTime);
        model.addAttribute("criteria", normalizedCriteria);
        model.addAttribute("chartType", normalizedChartType);

        // 1. Xác định khoảng thời gian lọc (giờ, ngày, tháng, năm)
        LocalDate today = LocalDate.now();
        LocalDateTime startDateTime;
        LocalDateTime endDateTime = today.atTime(LocalTime.MAX);

        switch (normalizedTime) {
            case "hour" -> startDateTime = today.atStartOfDay();
            case "month" -> startDateTime = today.withDayOfMonth(1).atStartOfDay();
            case "year" -> startDateTime = today.withDayOfYear(1).atStartOfDay();
            case "custom" -> {
                if (startDateStr != null && !startDateStr.isBlank() && endDateStr != null && !endDateStr.isBlank()) {
                    try {
                        LocalDate start = LocalDate.parse(startDateStr);
                        LocalDate end = LocalDate.parse(endDateStr);
                        if (start.isAfter(end)) {
                            LocalDate tmp = start;
                            start = end;
                            end = tmp;
                        }
                        startDateTime = start.atStartOfDay();
                        endDateTime = end.atTime(LocalTime.MAX);
                    } catch (Exception e) {
                        normalizedTime = "day";
                        startDateTime = today.minusDays(6).atStartOfDay();
                    }
                } else {
                    normalizedTime = "day";
                    startDateTime = today.minusDays(6).atStartOfDay();
                }
            }
            default -> { // "day"
                normalizedTime = "day";
                startDateTime = today.minusDays(6).atStartOfDay();
            }
        }

        try {
            // Lấy danh sách đơn hàng trong khoảng thời gian lọc
            List<Order> rangeOrders;
            try {
                rangeOrders = orderRepository.findByPickupStoreIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                        storeId, startDateTime, endDateTime);
            } catch (Exception e) {
                rangeOrders = Collections.emptyList();
            }

            // Doanh thu bán hoa (đã trừ hoàn tiền)
            BigDecimal grossFlowerRevenue = rangeOrders.stream()
                    .filter(o -> "PAID".equalsIgnoreCase(o.getPaymentStatus()) &&
                            (o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.COMPLETED))
                    .map(Order::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal refundAmount = rangeOrders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.REFUNDED)
                    .map(Order::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal flowerRevenue = grossFlowerRevenue.subtract(refundAmount).max(BigDecimal.ZERO);

            long completedOrdersCount = rangeOrders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.COMPLETED)
                    .count();

            List<Order> pendingOrdersList = allCurrentStoreOrders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.PENDING ||
                            o.getStatus() == OrderStatus.CONFIRMED ||
                            o.getStatus() == OrderStatus.PREPARING)
                    .toList();
            long pendingOrdersCount = pendingOrdersList.size();

            long lowStockCount = 0;
            try {
                lowStockCount = storeInventoryRepository.countByStoreIdAndQuantityLessThanEqual(storeId, 5);
            } catch (Exception e) {
                log.warn("Lỗi đếm tồn kho thấp: {}", e.getMessage());
            }

            // Bước 7: Tính toán dữ liệu biểu đồ theo giờ, ngày, tháng, năm cho cả 3 chỉ số
            List<String> chartLabels = new ArrayList<>();
            List<BigDecimal> chartRevenueValues = new ArrayList<>();
            List<Long> chartOrderValues = new ArrayList<>();
            List<Long> chartProductValues = new ArrayList<>();

            if ("hour".equals(normalizedTime)) {
                // 24 khung giờ
                for (int h = 0; h < 24; h++) {
                    LocalDateTime hStart = today.atTime(h, 0);
                    LocalDateTime hEnd = hStart.plusHours(1);
                    chartLabels.add(h + "h");

                    List<Order> hOrders = rangeOrders.stream()
                            .filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().isBefore(hStart) && o.getCreatedAt().isBefore(hEnd))
                            .toList();

                    BigDecimal hRev = hOrders.stream()
                            .filter(o -> "PAID".equalsIgnoreCase(o.getPaymentStatus()) &&
                                    (o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.COMPLETED))
                            .map(Order::getTotalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    chartRevenueValues.add(hRev);

                    chartOrderValues.add((long) hOrders.size());

                    long hProds = hOrders.stream()
                            .flatMap(o -> o.getOrderDetails() != null ? o.getOrderDetails().stream() : java.util.stream.Stream.empty())
                            .mapToLong(d -> d.getQuantity() != null ? d.getQuantity() : 1)
                            .sum();
                    chartProductValues.add(hProds);
                }
            } else if ("month".equals(normalizedTime)) {
                // Các ngày trong tháng
                LocalDate cursor = startDateTime.toLocalDate();
                LocalDate stop = endDateTime.toLocalDate();
                DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("dd/MM");
                while (!cursor.isAfter(stop)) {
                    LocalDateTime dStart = cursor.atStartOfDay();
                    LocalDateTime dEnd = cursor.atTime(LocalTime.MAX);
                    chartLabels.add(cursor.format(dayFmt));

                    LocalDate cur = cursor;
                    List<Order> dOrders = rangeOrders.stream()
                            .filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().isBefore(dStart) && !o.getCreatedAt().isAfter(dEnd))
                            .toList();

                    BigDecimal dRev = dOrders.stream()
                            .filter(o -> "PAID".equalsIgnoreCase(o.getPaymentStatus()) &&
                                    (o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.COMPLETED))
                            .map(Order::getTotalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    chartRevenueValues.add(dRev);
                    chartOrderValues.add((long) dOrders.size());
                    long dProds = dOrders.stream()
                            .flatMap(o -> o.getOrderDetails() != null ? o.getOrderDetails().stream() : java.util.stream.Stream.empty())
                            .mapToLong(d -> d.getQuantity() != null ? d.getQuantity() : 1)
                            .sum();
                    chartProductValues.add(dProds);
                    cursor = cursor.plusDays(1);
                }
            } else if ("year".equals(normalizedTime)) {
                // 12 tháng trong năm
                int curYear = today.getYear();
                for (int m = 1; m <= 12; m++) {
                    chartLabels.add("T" + m);
                    int monthVal = m;
                    List<Order> mOrders = rangeOrders.stream()
                            .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().getYear() == curYear && o.getCreatedAt().getMonthValue() == monthVal)
                            .toList();

                    BigDecimal mRev = mOrders.stream()
                            .filter(o -> "PAID".equalsIgnoreCase(o.getPaymentStatus()) &&
                                    (o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.COMPLETED))
                            .map(Order::getTotalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    chartRevenueValues.add(mRev);
                    chartOrderValues.add((long) mOrders.size());
                    long mProds = mOrders.stream()
                            .flatMap(o -> o.getOrderDetails() != null ? o.getOrderDetails().stream() : java.util.stream.Stream.empty())
                            .mapToLong(d -> d.getQuantity() != null ? d.getQuantity() : 1)
                            .sum();
                    chartProductValues.add(mProds);
                }
            } else {
                // Theo ngày ("day")
                LocalDate cursor = startDateTime.toLocalDate();
                LocalDate stop = endDateTime.toLocalDate();
                DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("dd/MM");
                while (!cursor.isAfter(stop)) {
                    LocalDateTime dStart = cursor.atStartOfDay();
                    LocalDateTime dEnd = cursor.atTime(LocalTime.MAX);
                    chartLabels.add(cursor.format(dayFmt));

                    List<Order> dOrders = rangeOrders.stream()
                            .filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().isBefore(dStart) && !o.getCreatedAt().isAfter(dEnd))
                            .toList();

                    BigDecimal dRev = dOrders.stream()
                            .filter(o -> "PAID".equalsIgnoreCase(o.getPaymentStatus()) &&
                                    (o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.COMPLETED))
                            .map(Order::getTotalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    chartRevenueValues.add(dRev);
                    chartOrderValues.add((long) dOrders.size());
                    long dProds = dOrders.stream()
                            .flatMap(o -> o.getOrderDetails() != null ? o.getOrderDetails().stream() : java.util.stream.Stream.empty())
                            .mapToLong(d -> d.getQuantity() != null ? d.getQuantity() : 1)
                            .sum();
                    chartProductValues.add(dProds);
                    cursor = cursor.plusDays(1);
                }
            }

            boolean hasRevenueData = chartRevenueValues.stream().anyMatch(v -> v.compareTo(BigDecimal.ZERO) > 0)
                    || chartOrderValues.stream().anyMatch(v -> v > 0);

            // Bước 8: Top sản phẩm bán chạy (cho tới 1000 sản phẩm theo Post-condition)
            List<TopFlowerDto> topProducts = new ArrayList<>();
            Map<Long, Integer> productQtyMap = new HashMap<>();
            Map<Long, BigDecimal> productRevenueMap = new HashMap<>();
            Map<Long, Product> productMap = new HashMap<>();

            for (Order o : allCurrentStoreOrders) {
                if ((o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.COMPLETED) && o.getOrderDetails() != null) {
                    for (OrderDetail d : o.getOrderDetails()) {
                        if (d != null && d.getProduct() != null) {
                            Long pId = d.getProduct().getId();
                            productMap.putIfAbsent(pId, d.getProduct());
                            int q = d.getQuantity() != null ? d.getQuantity() : 1;
                            productQtyMap.merge(pId, q, Integer::sum);
                            BigDecimal price = d.getUnitPrice() != null ? d.getUnitPrice() : BigDecimal.ZERO;
                            productRevenueMap.merge(pId, price.multiply(BigDecimal.valueOf(q)), BigDecimal::add);
                        }
                    }
                }
            }

            if (!productQtyMap.isEmpty()) {
                topProducts = productQtyMap.entrySet().stream()
                        .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                        .limit(1000)
                        .map(entry -> {
                            Product p = productMap.get(entry.getKey());
                            String catName = "Hoa tươi";
                            try {
                                if (p != null && p.getCategory() != null) catName = p.getCategory().getName();
                            } catch (Exception ignored) {}
                            return new TopFlowerDto(
                                    p != null ? p.getId() : entry.getKey(),
                                    p != null ? p.getName() : "Mẫu hoa chi nhánh",
                                    p != null ? p.getImageUrl() : null,
                                    catName,
                                    p != null ? p.getPriceBuy() : BigDecimal.ZERO,
                                    entry.getValue(),
                                    productRevenueMap.getOrDefault(entry.getKey(), BigDecimal.ZERO),
                                    p != null ? p.getRating() : 5.0
                            );
                        })
                        .toList();
            } else {
                topProducts = allStoreProducts.stream()
                        .limit(1000)
                        .map(p -> {
                            String catName = "Hoa tươi";
                            try {
                                if (p.getCategory() != null) catName = p.getCategory().getName();
                            } catch (Exception ignored) {}
                            int sold = p.getSold() != null ? p.getSold() : 0;
                            BigDecimal pPrice = p.getPriceBuy() != null ? p.getPriceBuy() : BigDecimal.ZERO;
                            return new TopFlowerDto(
                                    p.getId(),
                                    p.getName(),
                                    p.getImageUrl(),
                                    catName,
                                    pPrice,
                                    sold,
                                    pPrice.multiply(BigDecimal.valueOf(sold)),
                                    p.getRating()
                            );
                        })
                        .toList();
            }

            // Bước 8: Top đơn hàng mới nhất (lên đến 1000 đơn hàng theo Post-condition)
            List<Order> recentOrders = allCurrentStoreOrders.stream().limit(1000).toList();

            // Đổ dữ liệu vào model
            model.addAttribute("dataLoadError", false);
            model.addAttribute("flowerRevenue", flowerRevenue);
            model.addAttribute("rangeOrdersCount", rangeOrders.size());
            model.addAttribute("completedOrdersCount", completedOrdersCount);
            model.addAttribute("pendingOrdersCount", pendingOrdersCount);
            model.addAttribute("lowStockProductsCount", lowStockCount);
            model.addAttribute("chartLabels", chartLabels);
            model.addAttribute("chartRevenueValues", chartRevenueValues);
            model.addAttribute("chartOrderValues", chartOrderValues);
            model.addAttribute("chartProductValues", chartProductValues);
            model.addAttribute("chartValues", "orders".equals(normalizedCriteria) ? chartOrderValues : ("products".equals(normalizedCriteria) ? chartProductValues : chartRevenueValues));
            model.addAttribute("hasRevenueData", hasRevenueData);
            model.addAttribute("topProducts", topProducts);
            model.addAttribute("recentOrders", recentOrders);
            model.addAttribute("pendingOrdersList", pendingOrdersList.stream().limit(10).toList());

        } catch (Exception ex) {
            log.error("Lỗi tải dữ liệu dashboard chi nhánh " + storeId, ex);
            model.addAttribute("dataLoadError", true);
            model.addAttribute("errorMessage", "Không tải được dữ liệu thống kê chi nhánh. Vui lòng thử lại!");
            model.addAttribute("flowerRevenue", null);
            model.addAttribute("completedOrdersCount", null);
            model.addAttribute("pendingOrdersCount", null);
            model.addAttribute("lowStockProductsCount", null);
            model.addAttribute("chartLabels", Collections.emptyList());
            model.addAttribute("chartValues", Collections.emptyList());
            model.addAttribute("chartOrderValues", Collections.emptyList());
            model.addAttribute("chartProductValues", Collections.emptyList());
            model.addAttribute("hasRevenueData", false);
            model.addAttribute("topProducts", Collections.emptyList());
            model.addAttribute("recentOrders", Collections.emptyList());
            model.addAttribute("pendingOrdersList", Collections.emptyList());
        }

        return "manager/dashboard";
    }

    private Store resolveStore(User user) {
        if (user.getStore() != null) return user.getStore();
        List<Store> owned = storeRepository.findByOwnerId(user.getId());
        if (!owned.isEmpty()) {
            Store first = owned.get(0);
            user.setStore(first);
            userRepository.save(user);
            return first;
        }
        return null;
    }
}