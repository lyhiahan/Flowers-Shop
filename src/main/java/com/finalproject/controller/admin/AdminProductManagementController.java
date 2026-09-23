package com.finalproject.controller.admin;

import com.finalproject.entity.Product;
import com.finalproject.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/products-manage")
@RequiredArgsConstructor
public class AdminProductManagementController {

    private final ProductRepository productRepository;

    @GetMapping
    public String listProducts(Model model) {
        List<Product> products = productRepository.findAll();
        model.addAttribute("products", products);
        return "admin/products-manage";
    }

    @PostMapping("/toggle-active/{id}")
    public String toggleProductActive(@PathVariable("id") Long id, RedirectAttributes ra) {
        Product product = productRepository.findById(id).orElseThrow();
        product.setIsActive(!Boolean.TRUE.equals(product.getIsActive()));
        productRepository.save(product);
        ra.addFlashAttribute("successMessage", "Đã cập nhật kiểm duyệt sản phẩm [" + product.getName() + "] thành [" + (product.getIsActive() ? "Đã cấp phép" : "Bị cấm") + "]");
        return "redirect:/admin/products-manage";
    }
}
