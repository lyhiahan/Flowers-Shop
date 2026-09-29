package com.finalproject.controller.admin;

import com.finalproject.entity.Order;
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
    private final com.finalproject.service.AnalyticsService analyticsService;

    @GetMapping({"", "/dashboard"})
    public String showDashboard(
            @org.springframework.web.bind.annotation.RequestParam(value = "timeframe", defaultValue = "day") String timeframe,
            Principal principal,
            Model model) {
        User admin = userRepository.findByUsername(principal.getName()).orElse(null);

        long totalProducts = productRepository.count();
        long totalCategories = categoryRepository.count();
        long totalStores = storeRepository.count();
        long totalUsers = userRepository.count();
        long totalCustomers = userRepository.countByRole(Role.CUSTOMER);
        long totalManagers = userRepository.countByRole(Role.MANAGER);
        long totalVendors = userRepository.countByRole(Role.VENDOR);
        long totalShippers = userRepository.countByRole(Role.SHIPPER);

        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
        long rentingOrders = orderRepository.countByStatus(OrderStatus.RENTING);
        long completedOrders = orderRepository.countByStatus(OrderStatus.COMPLETED);

        List<Order> recentOrders = orderRepository.findTop10ByOrderByCreatedAtDesc();

        BigDecimal totalRevenue = analyticsService.getStoreRevenueStats(null).getTotalBuyRevenue();

        // Biểu đồ đa thời gian theo đề cương
        var revenueChart = analyticsService.getRevenueChart(timeframe, null);
        var usersChart = analyticsService.getNewUsersChart(timeframe);
        var storesChart = analyticsService.getNewStoresChart(timeframe);

        model.addAttribute("admin", admin);
        model.addAttribute("timeframe", timeframe);
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("totalCategories", totalCategories);
        model.addAttribute("totalStores", totalStores);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalCustomers", totalCustomers);
        model.addAttribute("totalManagers", totalManagers);
        model.addAttribute("totalVendors", totalVendors);
        model.addAttribute("totalShippers", totalShippers);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("rentingOrders", rentingOrders);
        model.addAttribute("completedOrders", completedOrders);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("recentOrders", recentOrders);
        model.addAttribute("revenueChart", revenueChart);
        model.addAttribute("usersChart", usersChart);
        model.addAttribute("storesChart", storesChart);
        model.addAttribute("products", productRepository.findAll());
        model.addAttribute("stores", storeRepository.findAll());

        return "admin/dashboard";
    }
}
