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
@RequestMapping({"/manager", "/vendor"})
@RequiredArgsConstructor
public class ManagerDashboardController {

    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;
    private final com.finalproject.service.StoreAccessService storeAccess;

    @GetMapping("/dashboard")
    public String showManagerDashboard(Principal principal, Model model) {
        User manager = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new AccessDeniedException("Người dùng không tồn tại"));

        if (manager.getStore() == null) {
            return "redirect:/manager/stores";
        }

        RevenueChartDto stats = analyticsService.getStoreRevenueStats(manager.getStore().getId());
        model.addAttribute("store", manager.getStore());
        model.addAttribute("stats", stats);
        return "manager/dashboard";
    }
}