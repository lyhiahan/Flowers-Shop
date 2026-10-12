package com.finalproject.controller.manager;

import com.finalproject.dto.response.ChartSeriesDto;
import com.finalproject.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
@RequestMapping({"/manager/analytics", "/vendor/analytics"})
@RequiredArgsConstructor
public class ManagerAnalyticsController {

    private final AnalyticsService analyticsService;
    private final com.finalproject.service.StoreAccessService storeAccess;
    private final java.time.Clock clock;

    @GetMapping
    public String showAnalytics(@RequestParam(value = "timeframe", required = false) String timeframe,
                                @RequestParam(value = "startDate", required = false) @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
                                @RequestParam(value = "endDate", required = false) @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate,
                                @RequestParam(value = "type", defaultValue = "order") String type,
                                Principal principal,
                                Model model) {
        var store = storeAccess.assignedStore();
        storeAccess.requireManager(store);
        Long storeId = store.getId();
        var range = com.finalproject.service.ReportRange.resolve(timeframe, startDate, endDate, clock);
        if (!java.util.Set.of("order", "product").contains(type)) throw new IllegalArgumentException("Loại báo cáo không hợp lệ.");

        ChartSeriesDto revenueChart;
        if ("product".equalsIgnoreCase(type)) {
            revenueChart = analyticsService.getProductRevenueChart(storeId, range);
        } else {
            revenueChart = analyticsService.getRevenueChart(range, storeId);
        }

        model.addAttribute("timeframe", timeframe == null ? range.period() : timeframe);
        model.addAttribute("reportRange", range);
        model.addAttribute("type", type);
        model.addAttribute("revenueChart", revenueChart);
        model.addAttribute("store", store);
        return "manager/analytics";
    }
}
