package com.finalproject.controller.admin;

import com.finalproject.dto.request.ProductForm;
import com.finalproject.repository.CategoryRepository;
import com.finalproject.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;

    @GetMapping
    public String listProducts(Model model) {
        model.addAttribute("products", productService.getAllProducts());
        return "admin/product-list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("productForm")) {
            model.addAttribute("productForm", new ProductForm());
        }
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/product-form";
    }

    @PostMapping("/create")
    public String handleCreateProduct(@Valid @ModelAttribute("productForm") ProductForm form,
                                      BindingResult bindingResult,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        model.addAttribute("categories", categoryRepository.findAll());

        if (bindingResult.hasErrors()) {
            return "admin/product-form";
        }

        try {
            productService.createProduct(form);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm sản phẩm mới thành công!");
            return "redirect:/admin/products";
        } catch (IllegalArgumentException ex) {
            if ("WARN_SELL_AT_LOSS".equals(ex.getMessage())) {
                // Bật cờ cảnh báo để hiển thị Modal Yes/No trên Thymeleaf
                model.addAttribute("showLossWarningModal", true);
                model.addAttribute("importPriceVal", form.getImportPrice());
                model.addAttribute("priceBuyVal", form.getPriceBuy());
                return "admin/product-form";
            }
            bindingResult.rejectValue("name", "error.product", ex.getMessage());
            return "admin/product-form";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Lỗi khi lưu sản phẩm: " + ex.getMessage());
            return "admin/product-form";
        }
    }
}