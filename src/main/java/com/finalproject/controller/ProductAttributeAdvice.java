package com.finalproject.controller;

import com.finalproject.service.ProductAttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice(basePackages={"com.finalproject.controller.admin", "com.finalproject.controller.manager"})
@RequiredArgsConstructor
public class ProductAttributeAdvice {
    private final ProductAttributeService attributes;
    @ModelAttribute public void attributes(HttpServletRequest request, Model model) {
        if (request.getRequestURI().contains("products")) model.addAttribute("attributeValues", attributes.activeValues());
    }
}
