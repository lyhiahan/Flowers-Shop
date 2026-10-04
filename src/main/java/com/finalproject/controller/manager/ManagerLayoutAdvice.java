package com.finalproject.controller.manager;

import com.finalproject.service.StoreAccessService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Selected shop label shared by list, detail and create/edit screens. */
@ControllerAdvice(basePackages = "com.finalproject.controller.manager")
@RequiredArgsConstructor
public class ManagerLayoutAdvice {
    private final StoreAccessService storeAccess;

    @ModelAttribute
    public void managerLayout(HttpServletRequest request, Model model) {
        if ("GET".equals(request.getMethod())) {
            model.addAttribute("managerStore", storeAccess.selectedStore());
        }
    }
}
