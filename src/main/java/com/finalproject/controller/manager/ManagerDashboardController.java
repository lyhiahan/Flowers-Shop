package com.finalproject.controller.manager;

import com.finalproject.service.*;
import com.finalproject.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@Controller @RequiredArgsConstructor
@RequestMapping({"/manager", "/vendor"})
public class ManagerDashboardController {
    private final StoreAccessService storeAccess;
    private final StoreRepository stores;
    private final AnalyticsService analytics;
    @GetMapping({"", "/dashboard"})
    public String showManagerDashboard(@RequestParam(value="storeId", required=false) Long storeId,
            @RequestParam(value="timeRange", defaultValue="day") String timeRange,
            @RequestParam(value="criteria", defaultValue="revenue") String criteria,
            @RequestParam(value="chartType", defaultValue="line") String chartType, Principal principal, Model model) {
        var store = storeId == null ? storeAccess.selectedStore() : stores.findById(storeId).orElseThrow();
        if (store == null) return "redirect:/manager/stores";
        storeAccess.requireManager(store);
        if (!java.util.Set.of("line", "bar", "pie").contains(chartType)) throw new IllegalArgumentException("Loại biểu đồ không hợp lệ.");
        if ("7days".equals(timeRange)) timeRange="day";
        if ("today".equals(timeRange)) timeRange="hour";
        model.addAllAttributes(analytics.dashboard(store.getId(), timeRange, criteria));
        model.addAttribute("store", store); model.addAttribute("accessibleStores", storeAccess.accessibleStores());
        model.addAttribute("currentUser", storeAccess.currentUser());
        model.addAttribute("timeRange", timeRange); model.addAttribute("criteria", criteria); model.addAttribute("chartType", chartType);

        for (String ranking : java.util.List.of("topProducts", "recentOrders", "topPointUsers", "topPointStores")) com.finalproject.utils.DashboardPages.apply(model, ranking);
        return "manager/dashboard";
    }
}
