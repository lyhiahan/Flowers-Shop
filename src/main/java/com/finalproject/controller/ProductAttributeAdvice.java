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
        if (request.getRequestURI().contains("products")) {
            var values = attributes.activeValues();
            model.addAttribute("attributeValues", values);
            var groups = new java.util.LinkedHashMap<String, java.util.List<com.finalproject.entity.StyleValue>>();
            values.forEach(value -> groups.computeIfAbsent(value.getStyle().getName(), key -> new java.util.ArrayList<>()).add(value));
            model.addAttribute("productAttributeGroups", groups);
        }
    }
}
