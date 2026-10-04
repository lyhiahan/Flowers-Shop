package com.finalproject.controller.admin;

import com.finalproject.entity.Order;
import com.finalproject.entity.Store;
import com.finalproject.entity.User;
import com.finalproject.enums.OrderStatus;
import com.finalproject.enums.Role;
import com.finalproject.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final RoleRequestRepository roleRequestRepository;
    private final com.finalproject.service.AnalyticsService analyticsService;

    @GetMapping({"", "/dashboard"})
    public String showDashboard(
            @org.springframework.web.bind.annotation.RequestParam(value = "timeframe", defaultValue = "day") String timeframe,
            @org.springframework.web.bind.annotation.RequestParam(value = "userChartType", defaultValue = "line") String userChartType,
            @org.springframework.web.bind.annotation.RequestParam(value = "storeChartType", defaultValue = "bar") String storeChartType,
            Principal principal,
            Model model) {
        try {
            User admin = userRepository.findByUsername(principal.getName()).orElse(null);

            long totalProducts = productRepository.count();
            long pendingProducts = productRepository.findAll().stream().filter(p -> Boolean.FALSE.equals(p.getIsActive())).count();
            long totalCategories = categoryRepository.count();
            long totalStores = storeRepository.count();
            long activeStores = storeRepository.countByStatus(true);
            long totalUsers = userRepository.count();
            long totalCustomers = userRepository.countByRole(Role.CUSTOMER);
            long totalManagers = userRepository.countByRole(Role.MANAGER);
            long totalVendors = userRepository.countByRole(Role.VENDOR);

            long totalOrders = orderRepository.count();
            long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
            long rentingOrders = orderRepository.countByStatus(OrderStatus.RENTING);
            long completedOrders = orderRepository.countByStatus(OrderStatus.COMPLETED);

            // Việc cần xử lý: Yêu cầu cấp vai trò, sản phẩm chờ duyệt
            long pendingRoleRequestsCount = roleRequestRepository.countByStatus(com.finalproject.enums.RoleRequestStatus.PENDING);
            List<com.finalproject.entity.RoleRequest> pendingRoleRequests = roleRequestRepository.findByStatusOrderByCreatedAtDesc(com.finalproject.enums.RoleRequestStatus.PENDING);
            List<com.finalproject.entity.Product> pendingApprovalProducts = productRepository.findAll().stream().filter(p -> Boolean.FALSE.equals(p.getIsActive())).limit(10).toList();

            List<Order> recentOrders;
            try {
                recentOrders = orderRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 10, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))).getContent();
            } catch (Exception e) {
                recentOrders = orderRepository.findTop10ByOrderByCreatedAtDesc();
            }

            List<com.finalproject.entity.Product> topProducts;
            try {
                topProducts = productRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 1000, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "sold"))).getContent();
            } catch (Exception e) {
                topProducts = java.util.Collections.emptyList();
            }

            // UC_025: Top người dùng điểm cao nhất hệ thống (lên đến 1000 người dùng)
            List<User> topPointUsers;
            try {
                topPointUsers = userRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 1000, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "point"))).getContent();
            } catch (Exception e) {
                topPointUsers = java.util.Collections.emptyList();
            }

            // UC_025: 1000 người dùng mới nhất
            List<User> recentUsers;
            try {
                recentUsers = userRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 1000, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"))).getContent();
            } catch (Exception e) {
                recentUsers = java.util.Collections.emptyList();
            }

            // UC_026: Top cửa hàng điểm cao nhất hệ thống (lên đến 1000 cửa hàng)
            List<Store> topPointStores;
            try {
                topPointStores = storeRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 1000, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "point"))).getContent();
            } catch (Exception e) {
                topPointStores = java.util.Collections.emptyList();
            }

            // UC_026: 1000 cửa hàng mới nhất
            List<Store> recentStores;
            try {
                recentStores = storeRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 1000, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"))).getContent();
            } catch (Exception e) {
                recentStores = java.util.Collections.emptyList();
            }

            BigDecimal totalRevenue = analyticsService.getStoreRevenueStats(null).getTotalBuyRevenue();

            // Biểu đồ đa thời gian theo đề cương
            var revenueChart = analyticsService.getRevenueChart(timeframe, null);
            var usersChart = analyticsService.getNewUsersChart(timeframe);
            var storesChart = analyticsService.getNewStoresChart(timeframe);

            model.addAttribute("dataLoadError", false);
            model.addAttribute("admin", admin);
            model.addAttribute("timeframe", timeframe);
            model.addAttribute("userChartType", userChartType != null ? userChartType.toLowerCase() : "line");
            model.addAttribute("storeChartType", storeChartType != null ? storeChartType.toLowerCase() : "bar");
            model.addAttribute("totalStores", totalStores);
            model.addAttribute("totalStoresCount", totalStores);
            model.addAttribute("activeStores", activeStores);
            model.addAttribute("totalProducts", totalProducts);
            model.addAttribute("totalProductsCount", totalProducts);
            model.addAttribute("pendingProducts", pendingProducts);
            model.addAttribute("pendingRoleRequestsCount", pendingRoleRequestsCount);
            model.addAttribute("pendingRoleRequests", pendingRoleRequests);
            model.addAttribute("pendingApprovalProducts", pendingApprovalProducts);
            model.addAttribute("totalCategories", totalCategories);
            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("totalUsersCount", totalUsers);
            model.addAttribute("totalCustomers", totalCustomers);
            model.addAttribute("totalManagers", totalManagers);
            model.addAttribute("totalVendors", totalVendors);
            model.addAttribute("totalOrders", totalOrders);
            model.addAttribute("totalOrdersCount", totalOrders);
            model.addAttribute("pendingOrders", pendingOrders);
            model.addAttribute("rentingOrders", rentingOrders);
            model.addAttribute("completedOrders", completedOrders);
            model.addAttribute("totalRevenue", totalRevenue);
            model.addAttribute("recentOrders", recentOrders);
            model.addAttribute("topProducts", topProducts);
            model.addAttribute("topPointUsers", topPointUsers);
            model.addAttribute("recentUsers", recentUsers);
            model.addAttribute("topPointStores", topPointStores);
            model.addAttribute("recentStores", recentStores);
            model.addAttribute("revenueChart", revenueChart);
            model.addAttribute("usersChart", usersChart);
            model.addAttribute("storesChart", storesChart);
            model.addAttribute("products", productRepository.findAll());
            model.addAttribute("stores", storeRepository.findAll());

        } catch (Exception ex) {
            model.addAttribute("dataLoadError", true);
            model.addAttribute("errorMessage", "Không thể tải dữ liệu thống kê hệ thống. Vui lòng thử lại!");
            model.addAttribute("totalUsers", 0L);
            model.addAttribute("totalUsersCount", 0L);
            model.addAttribute("totalStores", 0L);
            model.addAttribute("totalStoresCount", 0L);
            model.addAttribute("topPointUsers", java.util.Collections.emptyList());
            model.addAttribute("recentUsers", java.util.Collections.emptyList());
            model.addAttribute("topPointStores", java.util.Collections.emptyList());
            model.addAttribute("recentStores", java.util.Collections.emptyList());
        }

        return "admin/dashboard";
    }
}
