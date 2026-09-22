package com.finalproject.controller.manager;

import com.finalproject.dto.response.RevenueChartDto;
import com.finalproject.entity.User;
import com.finalproject.repository.UserRepository;
import com.finalproject.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerDashboardController {

    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;

    @GetMapping("/dashboard")
    public String showManagerDashboard(Principal principal, Model model) {
        User manager = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new AccessDeniedException("Người dùng không tồn tại"));

        if (manager.getStore() == null) {
            throw new AccessDeniedException("Quản lý chưa được gán vào chi nhánh nào!");
        }

        RevenueChartDto stats = analyticsService.getStoreRevenueStats(manager.getStore().getId());
        model.addAttribute("store", manager.getStore());
        model.addAttribute("stats", stats);
        return "manager/dashboard";
    }
}