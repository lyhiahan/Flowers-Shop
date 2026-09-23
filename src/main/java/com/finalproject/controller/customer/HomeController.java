package com.finalproject.controller.customer;

import com.finalproject.entity.Product;
import com.finalproject.repository.CategoryRepository;
import com.finalproject.repository.ProductRepository;
import com.finalproject.repository.StoreRepository;
import com.finalproject.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final CartService cartService;

    @GetMapping("/")
    public String index(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "tab", required = false) String tab,
            @RequestParam(value = "sort", required = false, defaultValue = "default") String sort,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "6") int size,
            Principal principal,
            Model model) {

        try {
            System.out.println("=== EXECUTING HomeController.index ===");
            // Guest: Luôn có danh sách sản phẩm bán trên 10 sản phẩm của các shop sắp xếp từ lớn đến nhỏ
            List<Product> guestFeatured = productRepository.findGuestFeaturedProducts();
            System.out.println("Guest featured count: " + (guestFeatured != null ? guestFeatured.size() : "null"));
            model.addAttribute("guestFeaturedProducts", guestFeatured);

        // 1. Sắp xếp (Sorting)
        Sort sortObj;
        if ("price_asc".equalsIgnoreCase(sort)) {
            sortObj = Sort.by(Sort.Direction.ASC, "priceBuy");
        } else if ("price_desc".equalsIgnoreCase(sort)) {
            sortObj = Sort.by(Sort.Direction.DESC, "priceBuy");
        } else if ("sold_desc".equalsIgnoreCase(sort)) {
            sortObj = Sort.by(Sort.Direction.DESC, "sold");
        } else if ("rating_desc".equalsIgnoreCase(sort)) {
            sortObj = Sort.by(Sort.Direction.DESC, "rating");
        } else {
            sortObj = Sort.by(Sort.Direction.DESC, "id");
        }

        // Top 20 tabs theo rubric đề cương (mới nhất, bán chạy, đánh giá, yêu thích)
        if ("newest".equalsIgnoreCase(tab)) {
            sortObj = Sort.by(Sort.Direction.DESC, "createdAt");
            size = 20;
        } else if ("bestseller".equalsIgnoreCase(tab)) {
            sortObj = Sort.by(Sort.Direction.DESC, "sold");
            size = 20;
        } else if ("toprated".equalsIgnoreCase(tab)) {
            sortObj = Sort.by(Sort.Direction.DESC, "rating");
            size = 20;
        }

        // 2. Phân trang (Pagination)
        int pageNumber = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageNumber, size, sortObj);

        // 3. Tìm kiếm theo từ khóa & Lọc theo danh mục
        Page<Product> productPage;
        boolean hasKeyword = (keyword != null && !keyword.trim().isEmpty());
        boolean hasCategory = (categoryId != null && categoryId > 0);

        String trimmedKeyword = hasKeyword ? keyword.trim() : null;

        if (hasCategory && hasKeyword) {
            productPage = productRepository.searchByCategoryAndKeyword(categoryId, trimmedKeyword, pageable);
        } else if (hasCategory) {
            productPage = productRepository.findByCategoryId(categoryId, pageable);
        } else if (hasKeyword) {
            productPage = productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(trimmedKeyword, trimmedKeyword, pageable);
        } else {
            productPage = productRepository.findByIsActiveTrueAndIsSellingTrue(pageable);
        }

        // 4. Đưa dữ liệu sang View
        model.addAttribute("productPage", productPage);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalItems", productPage.getTotalElements());
        model.addAttribute("keyword", trimmedKeyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("tab", tab);
        model.addAttribute("sort", sort);
        model.addAttribute("size", size);

        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("stores", storeRepository.findByStatusTrue());

        if (principal != null) {
            try {
                model.addAttribute("cartItemCount", cartService.getCartItemCount(principal.getName()));
            } catch (Exception ignored) {
            }
        }

        System.out.println("=== FINISHED HomeController.index, returning 'index' ===");
        return "index";
        } catch (Throwable t) {
            System.err.println("=== ERROR IN HomeController.index ===");
            t.printStackTrace();
            throw t;
        }
    }
}