package com.finalproject.controller.manager;

import com.finalproject.entity.*;
import com.finalproject.enums.OrderStatus;
import com.finalproject.repository.*;
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

@Controller
@RequestMapping({"/manager", "/vendor"})
@RequiredArgsConstructor
public class ManagerDashboardController {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final StoreInventoryRepository storeInventoryRepository;
    private final ProductRepository productRepository;

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

    @GetMapping("/dashboard")
    public String showManagerDashboard(
            @RequestParam(name = "timeRange", defaultValue = "7days") String timeRange,
            @RequestParam(name = "startDate", required = false) String startDateStr,
            @RequestParam(name = "endDate", required = false) String endDateStr,
            Principal principal,
            Model model) {

        User manager = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new AccessDeniedException("Người dùng không tồn tại"));

        if (manager.getStore() == null) {
            return "redirect:/manager/stores";
        }

        Long storeId = manager.getStore().getId();
        Store store = manager.getStore();

        // Rút gọn tên và địa điểm chi nhánh: ví dụ "Bến Thành · Quận 1"
        String storeShortTitle = "Chi Nhánh Tiệm Hoa";
        if (store.getName() != null) {
            String shortName = store.getName().replace("Chi Nhánh ", "").replace("Chi nhánh ", "").trim();
            if (shortName.contains("(") && shortName.contains(")")) {
                shortName = shortName.replace("(", "· ").replace(")", "").replaceAll("\\s+", " ").trim();
            }
            storeShortTitle = shortName;
        }

        model.addAttribute("store", store);
        model.addAttribute("storeShortTitle", storeShortTitle);
        model.addAttribute("lastUpdatedTime", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));

        // 1. Xác định khoảng thời gian lọc
        LocalDate today = LocalDate.now();
        LocalDateTime startDateTime;
        LocalDateTime endDateTime;

        if ("today".equalsIgnoreCase(timeRange)) {
            startDateTime = today.atStartOfDay();
            endDateTime = today.atTime(LocalTime.MAX);
        } else if ("month".equalsIgnoreCase(timeRange)) {
            startDateTime = today.withDayOfMonth(1).atStartOfDay();
            endDateTime = today.atTime(LocalTime.MAX);
        } else if ("custom".equalsIgnoreCase(timeRange) && startDateStr != null && !startDateStr.isBlank() && endDateStr != null && !endDateStr.isBlank()) {
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
                timeRange = "7days";
                startDateTime = today.minusDays(6).atStartOfDay();
                endDateTime = today.atTime(LocalTime.MAX);
            }
        } else {
            timeRange = "7days";
            startDateTime = today.minusDays(6).atStartOfDay();
            endDateTime = today.atTime(LocalTime.MAX);
        }

        try {
            // 2. Lấy đơn hàng trong khoảng thời gian được chọn
            List<Order> rangeOrders = orderRepository.findByPickupStoreIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                    storeId, startDateTime, endDateTime);

            // Quy tắc: Doanh thu bán hoa chỉ ghi nhận đơn đã giao/hoàn thành và đã thanh toán; trừ khoản hoàn tiền
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

            BigDecimal flowerRevenue = grossFlowerRevenue.subtract(refundAmount);
            if (flowerRevenue.compareTo(BigDecimal.ZERO) < 0) {
                flowerRevenue = BigDecimal.ZERO;
            }

            long completedOrdersCount = rangeOrders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.COMPLETED)
                    .count();

            // 3. Tình trạng hiện tại của chi nhánh
            List<Order> allCurrentStoreOrders = orderRepository.findByPickupStoreIdOrderByCreatedAtDesc(storeId);

            // Đơn chờ xử lý: PENDING, CONFIRMED, PREPARING
            List<Order> pendingOrdersList = allCurrentStoreOrders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.PENDING ||
                            o.getStatus() == OrderStatus.CONFIRMED ||
                            o.getStatus() == OrderStatus.PREPARING)
                    .toList();

            long pendingOrdersCount = pendingOrdersList.size();

            // Sản phẩm sắp hết hàng (tồn kho <= 5 tại chi nhánh)
            long lowStockCount = storeInventoryRepository.countByStoreIdAndQuantityLessThanEqual(storeId, 5);

            // 4. Biểu đồ doanh thu theo thời gian
            List<String> chartLabels = new ArrayList<>();
            List<BigDecimal> chartValues = new ArrayList<>();
            DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd/MM");

            if ("today".equalsIgnoreCase(timeRange)) {
                // Theo 24 khung giờ
                for (int h = 0; h < 24; h++) {
                    LocalDateTime hourStart = today.atTime(h, 0);
                    LocalDateTime hourEnd = hourStart.plusHours(1);
                    chartLabels.add(h + "h");

                    BigDecimal hourRev = rangeOrders.stream()
                            .filter(o -> "PAID".equalsIgnoreCase(o.getPaymentStatus()) &&
                                    (o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.COMPLETED) &&
                                    o.getCreatedAt() != null &&
                                    !o.getCreatedAt().isBefore(hourStart) &&
                                    o.getCreatedAt().isBefore(hourEnd))
                            .map(Order::getTotalAmount)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    chartValues.add(hourRev);
                }
            } else {
                // Theo từng ngày từ startDateTime tới endDateTime
                LocalDate cursor = startDateTime.toLocalDate();
                LocalDate stop = endDateTime.toLocalDate();
                while (!cursor.isAfter(stop)) {
                    LocalDateTime dayStart = cursor.atStartOfDay();
                    LocalDateTime dayEnd = cursor.atTime(LocalTime.MAX);
                    chartLabels.add(cursor.format(dayFormatter));

                    BigDecimal dayRev = rangeOrders.stream()
                            .filter(o -> "PAID".equalsIgnoreCase(o.getPaymentStatus()) &&
                                    (o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.COMPLETED) &&
                                    o.getCreatedAt() != null &&
                                    !o.getCreatedAt().isBefore(dayStart) &&
                                    !o.getCreatedAt().isAfter(dayEnd))
                            .map(Order::getTotalAmount)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    chartValues.add(dayRev);
                    cursor = cursor.plusDays(1);
                }
            }

            boolean hasRevenueData = chartValues.stream().anyMatch(v -> v.compareTo(BigDecimal.ZERO) > 0);

            // 5. Top 5 mẫu hoa bán chạy nhất
            Map<Long, Integer> productQtyMap = new HashMap<>();
            Map<Long, BigDecimal> productRevenueMap = new HashMap<>();
            Map<Long, Product> productMap = new HashMap<>();

            for (Order o : allCurrentStoreOrders) {
                if ((o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.COMPLETED) && o.getOrderDetails() != null) {
                    for (OrderDetail d : o.getOrderDetails()) {
                        if (d.getProduct() != null) {
                            Long pId = d.getProduct().getId();
                            productMap.putIfAbsent(pId, d.getProduct());
                            productQtyMap.merge(pId, d.getQuantity() != null ? d.getQuantity() : 1, Integer::sum);
                            BigDecimal subtotal = (d.getUnitPrice() != null ? d.getUnitPrice() : BigDecimal.ZERO)
                                    .multiply(BigDecimal.valueOf(d.getQuantity() != null ? d.getQuantity() : 1));
                            productRevenueMap.merge(pId, subtotal, BigDecimal::add);
                        }
                    }
                }
            }

            List<TopFlowerDto> topProducts = new ArrayList<>();
            if (!productQtyMap.isEmpty()) {
                topProducts = productQtyMap.entrySet().stream()
                        .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                        .limit(5)
                        .map(entry -> {
                            Product p = productMap.get(entry.getKey());
                            return new TopFlowerDto(
                                    p.getId(),
                                    p.getName(),
                                    p.getImageUrl(),
                                    p.getCategory() != null ? p.getCategory().getName() : "Hoa tươi",
                                    p.getPriceBuy(),
                                    entry.getValue(),
                                    productRevenueMap.getOrDefault(entry.getKey(), BigDecimal.ZERO),
                                    p.getRating()
                            );
                        })
                        .toList();
            } else {
                List<Product> storeProds = productRepository.findByStoreIdOrderByCreatedAtDesc(storeId);
                topProducts = storeProds.stream()
                        .limit(5)
                        .map(p -> new TopFlowerDto(
                                p.getId(),
                                p.getName(),
                                p.getImageUrl(),
                                p.getCategory() != null ? p.getCategory().getName() : "Hoa tươi",
                                p.getPriceBuy(),
                                p.getSold() != null ? p.getSold() : 0,
                                p.getPriceBuy().multiply(BigDecimal.valueOf(p.getSold() != null ? p.getSold() : 0)),
                                p.getRating()
                        ))
                        .toList();
            }

            // Đổ dữ liệu thành công vào Model
            model.addAttribute("dataLoadError", false);
            model.addAttribute("flowerRevenue", flowerRevenue);
            model.addAttribute("completedOrdersCount", completedOrdersCount);
            model.addAttribute("pendingOrdersCount", pendingOrdersCount);
            model.addAttribute("lowStockProductsCount", lowStockCount);
            model.addAttribute("chartLabels", chartLabels);
            model.addAttribute("chartValues", chartValues);
            model.addAttribute("hasRevenueData", hasRevenueData);
            model.addAttribute("topProducts", topProducts);
            model.addAttribute("pendingOrdersList", pendingOrdersList.stream().limit(6).toList());

        } catch (Exception ex) {
            model.addAttribute("dataLoadError", true);
            model.addAttribute("flowerRevenue", null);
            model.addAttribute("completedOrdersCount", null);
            model.addAttribute("pendingOrdersCount", null);
            model.addAttribute("lowStockProductsCount", null);
            model.addAttribute("chartLabels", Collections.emptyList());
            model.addAttribute("chartValues", Collections.emptyList());
            model.addAttribute("hasRevenueData", false);
            model.addAttribute("topProducts", Collections.emptyList());
            model.addAttribute("pendingOrdersList", Collections.emptyList());
        }

        return "manager/dashboard";
    }
}