package com.finalproject.controller.manager;

import com.finalproject.dto.response.ChartSeriesDto;
import com.finalproject.entity.User;
import com.finalproject.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final com.finalproject.service.StoreAccessService storeAccess;

    @GetMapping
    public String showAnalytics(@RequestParam(value = "timeframe", defaultValue = "day") String timeframe,
                                @RequestParam(value = "type", defaultValue = "order") String type,
                                Principal principal,
                                Model model) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Long storeId = storeAccess.assignedStore().getId();

        ChartSeriesDto revenueChart;
        if ("product".equalsIgnoreCase(type)) {
            revenueChart = analyticsService.getProductRevenueChart(storeId);
        } else {
            revenueChart = analyticsService.getRevenueChart(timeframe, storeId);
        }

        model.addAttribute("timeframe", timeframe);
        model.addAttribute("type", type);
        model.addAttribute("revenueChart", revenueChart);
        model.addAttribute("store", user.getStore());
        return "manager/analytics";
    }
}
