package com.finalproject.controller.staff;

import com.finalproject.service.StoreAccessService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "com.finalproject.controller.staff")
@RequiredArgsConstructor
public class StaffLayoutAdvice {
    private final StoreAccessService access;

    @ModelAttribute
    public void layout(HttpServletRequest request, Model model) {
        if ("GET".equals(request.getMethod())) {
            model.addAttribute("staffUser", access.currentUser());
            model.addAttribute("staffStore", access.selectedStore());
        }
    }
}
