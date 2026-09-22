package com.finalproject.controller.customer;

import com.finalproject.repository.ProductImageRepository;
import com.finalproject.repository.ProductRepository;
import com.finalproject.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final ProductImageRepository productImageRepository;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("products", productRepository.findAll());
        model.addAttribute("stores", storeRepository.findByStatusTrue());
        return "index";
    }
}