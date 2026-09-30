package com.finalproject.controller.customer;

import com.finalproject.entity.Category;
import com.finalproject.entity.Product;
import com.finalproject.repository.CategoryRepository;
import com.finalproject.repository.ProductRepository;
import com.finalproject.repository.StoreRepository;
import com.finalproject.service.CartService;
import com.finalproject.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final CartService cartService;
    private final ProductService productService;
    private final com.finalproject.repository.UserRepository userRepository;

    @GetMapping({"/", "/products", "/search"})
    public String index(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "tab", required = false) String tab,
            @RequestParam(value = "sort", required = false, defaultValue = "default") String sort,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "minRating", required = false) Double minRating,
            @RequestParam(value = "storeId", required = false) Long storeId,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "6") int size,
            Principal principal,
            Model model) {

        try {
            // Guest Featured Products (Bán trên 10 sản phẩm sắp xếp giảm dần)
            List<Product> guestFeatured = productRepository.findGuestFeaturedProducts();
            model.addAttribute("guestFeaturedProducts", guestFeatured);

            if (principal != null) {
                userRepository.findByUsername(principal.getName()).ifPresent(u -> {
                    var followed = productService.getWishlist(u.getId()).stream()
                            .map(Product::getId)
                            .collect(java.util.stream.Collectors.toSet());
                    model.addAttribute("followedProductIds", followed);
                });
            }

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
            } else if ("name_asc".equalsIgnoreCase(sort)) {
                sortObj = Sort.by(Sort.Direction.ASC, "name");
            } else if ("name_desc".equalsIgnoreCase(sort)) {
                sortObj = Sort.by(Sort.Direction.DESC, "name");
            } else {
                sortObj = Sort.by(Sort.Direction.DESC, "id");
            }

            // Top 20 tabs
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

            // 3. Xử lý danh mục cha - con (Category Hierarchy)
            List<Long> filterCategoryIds = null;
            Category selectedCategory = null;
            List<Category> subCategories = new ArrayList<>();
            Category parentCategory = null;

            if (categoryId != null && categoryId > 0) {
                selectedCategory = categoryRepository.findById(categoryId).orElse(null);
                if (selectedCategory != null) {
                    filterCategoryIds = new ArrayList<>();
                    filterCategoryIds.add(selectedCategory.getId());

                    // Lấy các danh mục con của category được chọn
                    List<Category> children = categoryRepository.findByParentId(selectedCategory.getId());
                    if (children != null && !children.isEmpty()) {
                        subCategories.addAll(children);
                        for (Category child : children) {
                            filterCategoryIds.add(child.getId());
                        }
                    }

                    // Nếu category hiện tại là category con -> lấy category cha của nó
                    if (selectedCategory.getParent() != null) {
                        parentCategory = selectedCategory.getParent();
                        // Đồng thời lấy các sub-category cùng cấp để người dùng dễ chọn
                        List<Category> siblings = categoryRepository.findByParentId(parentCategory.getId());
                        if (siblings != null && !siblings.isEmpty()) {
                            subCategories = siblings;
                        }
                    }
                }
            }

            String trimmedKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

            // 4. Lọc sản phẩm (Filter Products)
            Page<Product> productPage = productRepository.filterProducts(
                    filterCategoryIds,
                    trimmedKeyword,
                    minPrice,
                    maxPrice,
                    minRating,
                    storeId,
                    pageable
            );

            // 5. Đưa dữ liệu sang View
            model.addAttribute("productPage", productPage);
            model.addAttribute("products", productPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", productPage.getTotalPages());
            model.addAttribute("totalItems", productPage.getTotalElements());

            model.addAttribute("keyword", trimmedKeyword);
            model.addAttribute("categoryId", categoryId);
            model.addAttribute("selectedCategory", selectedCategory);
            model.addAttribute("parentCategory", parentCategory);
            model.addAttribute("subCategories", subCategories);

            model.addAttribute("tab", tab);
            model.addAttribute("sort", sort);
            model.addAttribute("minPrice", minPrice);
            model.addAttribute("maxPrice", maxPrice);
            model.addAttribute("minRating", minRating);
            model.addAttribute("storeId", storeId);
            model.addAttribute("size", size);

            // Discover Categories: Danh mục gốc (parent_id IS NULL)
            List<Category> rootCategories = categoryRepository.findByParentIsNull();
            if (rootCategories == null || rootCategories.isEmpty()) {
                rootCategories = categoryRepository.findAll();
            }
            model.addAttribute("discoverCategories", rootCategories);
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("stores", storeRepository.findByStatusTrue());

            if (principal != null) {
                try {
                    model.addAttribute("cartItemCount", cartService.getCartItemCount(principal.getName()));
                } catch (Exception ignored) {
                }
            }

            return "index";
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }

    @GetMapping("/following")
    public String redirectFollowing(@RequestParam(value = "tab", required = false) String tab) {
        if (tab != null && !tab.isEmpty()) {
            return "redirect:/customer/following?tab=" + tab;
        }
        return "redirect:/customer/following";
    }
}