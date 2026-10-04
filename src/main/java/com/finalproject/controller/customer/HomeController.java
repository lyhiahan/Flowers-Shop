package com.finalproject.controller.customer;

import com.finalproject.repository.ProductSpecification;
import org.springframework.data.jpa.domain.Specification;
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

import lombok.extern.slf4j.Slf4j;
import java.util.Collections;

@Slf4j
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
            @RequestParam(value = "flowerStyle", required = false) List<String> flowerStyle,
            @RequestParam(value = "flowerType", required = false) List<String> flowerType,
            @RequestParam(value = "flowerColor", required = false) List<String> flowerColor,
            @RequestParam(value = "flowerOccasion", required = false) List<String> flowerOccasion,
            @RequestParam(value = "tab", required = false) String tab,
            @RequestParam(value = "sort", required = false, defaultValue = "default") String sort,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "minRating", required = false) Double minRating,
            @RequestParam(value = "storeId", required = false) Long storeId,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "12") int size,
            Principal principal,
            Model model) {

        try {
            model.addAttribute("hasActiveFilter", false);

            // Guest Featured Products: Tối đa 8 sản phẩm bán chạy nhất (2 hàng x 4 cột)
            List<Product> guestFeatured = productService.getTop8BestSellingProducts();
            if (guestFeatured != null && guestFeatured.size() > 8) {
                guestFeatured = guestFeatured.subList(0, 8);
            }
            model.addAttribute("guestFeaturedProducts", guestFeatured);

            if (principal != null) {
                userRepository.findByUsername(principal.getName()).ifPresent(u -> {
                    var followed = productService.getWishlist(u.getId()).stream()
                            .map(Product::getId)
                            .collect(java.util.stream.Collectors.toSet());
                    model.addAttribute("followedProductIds", followed);
                });
            }

            // Trim các tham số tìm kiếm & lọc trước khi dùng (phải đặt trước sort để tránh NPE)
            String trimmedKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
            List<String> cleanStyles = sanitizeFilterList(flowerStyle);
            List<String> cleanTypes = sanitizeFilterList(flowerType);
            List<String> cleanColors = sanitizeFilterList(flowerColor);
            List<String> cleanOccasions = sanitizeFilterList(flowerOccasion);

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
            } else if (trimmedKeyword != null) {
                sortObj = Sort.unsorted();
            } else {
                sortObj = Sort.by(Sort.Direction.DESC, "id");
            }

            // Tabs theo tiêu chí (Bán chạy nhất phân trang 12 sản phẩm/trang)
            if ("newest".equalsIgnoreCase(tab)) {
                sortObj = Sort.by(Sort.Direction.DESC, "createdAt");
                size = 12;
            } else if ("bestseller".equalsIgnoreCase(tab)) {
                sortObj = Sort.by(Sort.Direction.DESC, "sold");
                size = 12;
            } else if ("toprated".equalsIgnoreCase(tab)) {
                sortObj = Sort.by(Sort.Direction.DESC, "rating");
                size = 12;
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
                if (selectedCategory == null || Boolean.TRUE.equals(selectedCategory.getIsDeleted())) {
                    return "redirect:/products";
                }
                if (selectedCategory != null) {
                    filterCategoryIds = new ArrayList<>();
                    filterCategoryIds.add(selectedCategory.getId());

                    // Lấy các danh mục con của category được chọn
                    List<Category> children = categoryRepository.findActiveChildren(selectedCategory.getId());
                    if (children != null && !children.isEmpty()) {
                        subCategories.addAll(children);
                        for (Category child : children) {
                            filterCategoryIds.add(child.getId());
                        }
                    }

                    // Nếu category hiện tại là category con -> lấy category cha của nó
                    if (selectedCategory.getParent() != null && !Boolean.TRUE.equals(selectedCategory.getParent().getIsDeleted())) {
                        parentCategory = selectedCategory.getParent();
                        // Đồng thời lấy các sub-category cùng cấp để người dùng dễ chọn
                        List<Category> siblings = categoryRepository.findActiveChildren(parentCategory.getId());
                        if (siblings != null && !siblings.isEmpty()) {
                            subCategories = siblings;
                        }
                    }
                }
            }

            // 4. Lọc sản phẩm đa thuộc tính (Kiểu dáng, Loại hoa, Màu sắc, Dịp tặng, Danh mục, Giá, v.v.)
            Specification<Product> spec = ProductSpecification.filter(
                    filterCategoryIds,
                    trimmedKeyword,
                    minPrice,
                    maxPrice,
                    minRating,
                    storeId,
                    cleanStyles,
                    cleanTypes,
                    cleanColors,
                    cleanOccasions
            );
            Page<Product> productPage = productRepository.findAll(spec, pageable);

            // Xác định tiêu đề hiển thị chuẩn xác
            String activeFilterTitle = "Tất cả mẫu hoa";
            if (trimmedKeyword != null) {
                activeFilterTitle = "Kết quả cho “" + trimmedKeyword + "”";
            } else if ("bestseller".equalsIgnoreCase(tab)) {
                activeFilterTitle = "Hoa bán chạy nhất";
            } else if ("newest".equalsIgnoreCase(tab)) {
                activeFilterTitle = "Hoa mới nhất";
            } else if ("toprated".equalsIgnoreCase(tab)) {
                activeFilterTitle = "Hoa đánh giá cao";
            } else if (!cleanStyles.isEmpty()) {
                activeFilterTitle = String.join(", ", cleanStyles);
            } else if (selectedCategory != null) {
                activeFilterTitle = selectedCategory.getName();
            } else if (!cleanTypes.isEmpty()) {
                activeFilterTitle = String.join(", ", cleanTypes);
            } else if (!cleanOccasions.isEmpty()) {
                activeFilterTitle = "Hoa " + String.join(", ", cleanOccasions);
            }

            boolean hasActiveFilter = (!cleanStyles.isEmpty())
                    || (!cleanTypes.isEmpty())
                    || (!cleanColors.isEmpty())
                    || (!cleanOccasions.isEmpty())
                    || (categoryId != null && categoryId > 0)
                    || (trimmedKeyword != null)
                    || (minPrice != null)
                    || (maxPrice != null)
                    || (minRating != null)
                    || (tab != null && !tab.trim().isEmpty());

            // 5. Đưa dữ liệu sang View
            model.addAttribute("productPage", productPage);
            model.addAttribute("products", productPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", productPage.getTotalPages());
            model.addAttribute("totalItems", productPage.getTotalElements());

            model.addAttribute("keyword", trimmedKeyword);
            model.addAttribute("flowerStyles", cleanStyles);
            model.addAttribute("flowerTypes", cleanTypes);
            model.addAttribute("flowerColors", cleanColors);
            model.addAttribute("flowerOccasions", cleanOccasions);
            model.addAttribute("flowerStyle", cleanStyles.isEmpty() ? null : String.join(", ", cleanStyles));
            model.addAttribute("flowerType", cleanTypes.isEmpty() ? null : String.join(", ", cleanTypes));
            model.addAttribute("flowerColor", cleanColors.isEmpty() ? null : String.join(", ", cleanColors));
            model.addAttribute("flowerOccasion", cleanOccasions.isEmpty() ? null : String.join(", ", cleanOccasions));
            model.addAttribute("activeFilterTitle", activeFilterTitle);
            model.addAttribute("hasActiveFilter", hasActiveFilter);

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
            List<Category> rootCategories = categoryRepository.findActiveRootCategories();
            if (rootCategories == null || rootCategories.isEmpty()) {
                rootCategories = categoryRepository.findActiveCategories();
            }
            model.addAttribute("discoverCategories", rootCategories);
            model.addAttribute("categories", categoryRepository.findActiveCategories());
            model.addAttribute("stores", storeRepository.findByStatusTrue());

            if (principal != null) {
                try {
                    model.addAttribute("cartItemCount", cartService.getCartItemCount(principal.getName()));
                } catch (Exception ignored) {
                }
            }

            return "index";
        } catch (Throwable t) {
            log.error("Lỗi khi tải danh sách sản phẩm", t);
            model.addAttribute("loadError", "Không thể tải danh sách mẫu hoa vào lúc này. Vui lòng tải lại trang hoặc thử lại sau.");
            model.addAttribute("products", Collections.emptyList());
            model.addAttribute("totalItems", 0L);
            model.addAttribute("totalPages", 0);
            model.addAttribute("currentPage", 1);
            model.addAttribute("hasActiveFilter", false);
            model.addAttribute("activeFilterTitle", "Danh Sách Mẫu Hoa");
            return "index";
        }
    }

    private List<String> sanitizeFilterList(List<String> rawList) {
        if (rawList == null || rawList.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        for (String item : rawList) {
            if (item != null) {
                for (String sub : item.split(",")) {
                    String trimmed = sub.trim();
                    if (!trimmed.isEmpty() && !result.contains(trimmed)) {
                        result.add(trimmed);
                    }
                }
            }
        }
        return result;
    }

    @GetMapping("/following")
    public String redirectFollowing(@RequestParam(value = "tab", required = false) String tab) {
        if (tab != null && !tab.isEmpty()) {
            return "redirect:/customer/following?tab=" + tab;
        }
        return "redirect:/customer/following";
    }
}
