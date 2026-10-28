package com.finalproject.controller.admin;

import com.finalproject.entity.*;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import com.finalproject.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.time.*;
import java.util.*;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final AnalyticsService analyticsService;
    private final Clock clock;

    @GetMapping("/dashboard/stock")
    @ResponseBody
    public org.springframework.http.ResponseEntity<Map<String, Long>> stock() {
        return org.springframework.http.ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(Map.of("currentStockQuantity", analyticsService.currentStockQuantity(null)));
    }

    @GetMapping({"", "/dashboard"})
    public String showDashboard(
            @RequestParam(value = "timeframe", required = false) String timeframe,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "userChartType", defaultValue = "line") String userChartType,
            @RequestParam(value = "storeChartType", defaultValue = "bar") String storeChartType,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "criteria", defaultValue = "revenue") String criteria,
            @RequestParam(value = "chartType", defaultValue = "line") String chartType,
            Principal principal, Model model) {
        ReportRange range = ReportRange.resolve(timeframe, startDate, endDate, clock);
        if (!Set.of("line", "bar", "pie").contains(chartType) || !Set.of("line", "bar", "pie").contains(userChartType)
                || !Set.of("line", "bar", "pie").contains(storeChartType))
            throw new IllegalArgumentException("Loại biểu đồ không hợp lệ.");
        model.addAttribute("reportRange", range);
        model.addAttribute("timeframe", timeframe == null ? range.period() : timeframe);
        model.addAttribute("userChartType", userChartType); model.addAttribute("storeChartType", storeChartType);
        model.addAttribute("criteria", criteria); model.addAttribute("chartType", chartType);
        try {
            if ("admin_shopping_denied".equalsIgnoreCase(error))
                model.addAttribute("errorMessage", "Tài khoản Quản trị viên không thể thực hiện chức năng mua hàng hoặc truy cập giỏ hàng. Quý khách vui lòng quản trị hệ thống tại đây.");
            model.addAttribute("admin", userRepository.findByUsername(principal.getName()).orElse(null));
            Specification<User> registered = (root, query, cb) -> cb.and(
                    cb.greaterThanOrEqualTo(root.get("createdAt"), range.start()), cb.lessThan(root.get("createdAt"), range.endExclusive()),
                    range.bucket() == ReportRange.Bucket.HOUR ? cb.isTrue(root.get("creationTimeKnown")) : cb.conjunction());
            Specification<Store> createdStores = (root, query, cb) -> cb.and(
                    cb.greaterThanOrEqualTo(root.get("createdAt"), range.start()), cb.lessThan(root.get("createdAt"), range.endExclusive()));
            Specification<Product> createdProducts = (root, query, cb) -> cb.and(
                    cb.greaterThanOrEqualTo(root.get("createdAt"), range.start()), cb.lessThan(root.get("createdAt"), range.endExclusive()),
                    cb.or(cb.isFalse(root.get("isDeleted")), cb.isNull(root.get("isDeleted"))));
            Specification<Order> createdOrders = (root, query, cb) -> cb.and(
                    cb.greaterThanOrEqualTo(root.get("createdAt"), range.start()), cb.lessThan(root.get("createdAt"), range.endExclusive()));
            var latest = PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
            var byPoints = PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "point", "id"));
            model.addAllAttributes(analyticsService.dashboard(null, range, criteria));
            long totalUsers = userRepository.count(registered), totalStores = storeRepository.count(createdStores);
            model.addAttribute("totalUsers", totalUsers); model.addAttribute("totalUsersCount", totalUsers);
            model.addAttribute("totalStores", totalStores); model.addAttribute("totalStoresCount", totalStores);
            model.addAttribute("activeStores", storeRepository.count(createdStores.and((r, q, cb) -> cb.isTrue(r.get("status")))));
            model.addAttribute("totalProducts", model.getAttribute("totalProductsCount"));
            model.addAttribute("totalOrders", model.getAttribute("totalOrdersCount"));
            model.addAttribute("totalCustomers", userRepository.count(registered.and((r, q, cb) -> cb.equal(r.get("role"), Role.USER))));
            long managers = userRepository.count(registered.and((r, q, cb) -> cb.equal(r.get("role"), Role.MANAGER)));
            model.addAttribute("totalManagers", managers); model.addAttribute("totalVendors", managers);
            model.addAttribute("pendingOrders", orderRepository.count(createdOrders.and((r, q, cb) -> cb.equal(r.get("status"), OrderStatus.PENDING))));
            model.addAttribute("completedOrders", orderRepository.count(createdOrders.and((r, q, cb) -> cb.equal(r.get("status"), OrderStatus.COMPLETED))));
            var pendingProducts = createdProducts.and((r, q, cb) -> cb.isFalse(r.get("isActive")));
            model.addAttribute("pendingProducts", productRepository.count(pendingProducts));
            model.addAttribute("pendingApprovalProducts", productRepository.findAll(pendingProducts, PageRequest.of(0, 10)).getContent());
            model.addAttribute("totalCategories", categoryRepository.count());
            model.addAttribute("topPointUsers", userRepository.findAll(registered, byPoints).getContent());
            model.addAttribute("recentUsers", userRepository.findAll(registered, latest).getContent());
            model.addAttribute("topPointStores", storeRepository.findAll(createdStores, byPoints).getContent());
            model.addAttribute("recentStores", storeRepository.findAll(createdStores, latest).getContent());
            model.addAttribute("revenueChart", analyticsService.getRevenueChart(range, null));
            model.addAttribute("usersChart", analyticsService.getNewUsersChart(range));
            model.addAttribute("storesChart", analyticsService.getNewStoresChart(range));
            model.addAttribute("products", productRepository.findAll(createdProducts, latest).getContent());
            model.addAttribute("stores", storeRepository.findAll(createdStores, latest).getContent());
            model.addAttribute("dataLoadError", false);
        } catch (IllegalArgumentException ex) { throw ex; }
        catch (Exception ex) {
            model.addAttribute("dataLoadError", true);
            model.addAttribute("errorMessage", "Không thể tải dữ liệu thống kê hệ thống. Vui lòng thử lại!");
            for (String name : List.of("topProducts", "recentOrders", "topPointUsers", "recentUsers", "topPointStores", "recentStores"))
                model.addAttribute(name, Collections.emptyList());
        }
        for (String name : List.of("topProducts", "recentOrders", "topPointUsers", "topPointStores"))
            com.finalproject.utils.DashboardPages.apply(model, name);
        return "admin/dashboard";
    }
}
