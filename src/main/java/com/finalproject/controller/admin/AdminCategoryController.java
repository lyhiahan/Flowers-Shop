package com.finalproject.controller.admin;

import com.finalproject.entity.*;
import com.finalproject.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminCategoryController {
    private final com.finalproject.service.CategoryService categoryService;
    private final CategoryRepository categoryRepository;

    // --- 1. Quản lý Loại sản phẩm / Danh mục (Categories - UC_030) ---
    @GetMapping({"/categories", "/category", "/loai-san-pham"})
    public String listCategories(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {
        try {
            java.util.List<Category> allCategories = categoryRepository.findAll();

            java.util.List<Category> filtered = allCategories;
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = keyword.trim().toLowerCase();
                filtered = filtered.stream().filter(c ->
                        (c.getName() != null && c.getName().toLowerCase().contains(kw))
                        || (c.getDescription() != null && c.getDescription().toLowerCase().contains(kw))
                        || String.valueOf(c.getId()).contains(kw)
                ).toList();
            }

            // Phân trang danh sách loại sản phẩm (Bước 4)
            int totalElements = filtered.size();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            if (totalPages == 0) totalPages = 1;
            int fromIndex = Math.min(page * size, totalElements);
            int toIndex = Math.min(fromIndex + size, totalElements);
            java.util.List<Category> pagedCategories = filtered.subList(fromIndex, toIndex);

            // Cây danh mục (Bước 5: Review cây category)
            java.util.List<Category> rootCategories = allCategories.stream()
                    .filter(c -> c.getParent() == null)
                    .toList();

            model.addAttribute("categories", pagedCategories);
            model.addAttribute("allCategories", allCategories);
            model.addAttribute("rootCategories", rootCategories);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalElements", (long) totalElements);
            model.addAttribute("pageSize", size);
            model.addAttribute("keyword", keyword);
            model.addAttribute("dataLoadError", false);

        } catch (Exception ex) {
            model.addAttribute("dataLoadError", true);
            model.addAttribute("errorMessage", "Không thể tải danh sách loại sản phẩm. Vui lòng thử lại!");
            model.addAttribute("categories", java.util.Collections.emptyList());
            model.addAttribute("allCategories", java.util.Collections.emptyList());
            model.addAttribute("rootCategories", java.util.Collections.emptyList());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalElements", 0L);
            model.addAttribute("pageSize", size);
        }
        return "admin/categories";
    }

    /**
     * Bước 6 & 7: Chuyển đến trang form tạo category mới
     */
    @GetMapping("/categories/create")
    public String createCategoryForm(Model model) {
        model.addAttribute("category", new Category());
        model.addAttribute("allCategories", categoryRepository.findAll());
        model.addAttribute("isEdit", false);
        return "admin/category-form";
    }

    /**
     * Alternative 6a & 7a: Chuyển đến trang form chỉnh sửa category
     */
    @GetMapping("/categories/edit/{id}")
    public String editCategoryForm(@PathVariable("id") Long id, Model model) {
        Category cat = categoryRepository.findById(id).orElse(null);
        if (cat == null) {
            return "redirect:/admin/categories";
        }
        model.addAttribute("category", cat);
        model.addAttribute("allCategories", categoryRepository.findAll().stream().filter(c -> !c.getId().equals(id)).toList());
        model.addAttribute("isEdit", true);
        return "admin/category-form";
    }

    /**
     * Bước 8 - 12: Thêm mới category
     */
    @PostMapping("/categories/add")
    public String addCategory(@RequestParam("name") String name,
                              @RequestParam(value = "description", required = false) String description,
                              @RequestParam(value = "parentId", required = false) Long parentId,
                              RedirectAttributes ra) {
        try {
            categoryService.save(null, name, description, parentId);
            ra.addFlashAttribute("successMessage", "Thêm mới loại sản phẩm [" + name + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể thêm loại sản phẩm: " + ex.getMessage());
        }
        return "redirect:/admin/categories";
    }

    /**
     * Cập nhật category
     */
    @PostMapping("/categories/update/{id}")
    public String updateCategory(@PathVariable("id") Long id,
                                 @RequestParam("name") String name,
                                 @RequestParam(value = "description", required = false) String description,
                                 @RequestParam(value = "parentId", required = false) Long parentId,
                                 RedirectAttributes ra) {
        try {
            categoryService.save(id, name, description, parentId);
            ra.addFlashAttribute("successMessage", "Cập nhật loại sản phẩm [" + name + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể cập nhật loại sản phẩm: " + ex.getMessage());
        }
        return "redirect:/admin/categories";
    }

    /**
     * Alternative 6-9b: Xóa category (đánh dấu xóa mềm isDeleted = true)
     */
    @PostMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            Category cat = categoryRepository.findById(id).orElse(null);
            if (cat == null) {
                ra.addFlashAttribute("errorMessage", "Không tìm thấy loại sản phẩm cần xóa!");
                return "redirect:/admin/categories";
            }
            String catName = cat.getName();
            cat.setIsDeleted(true);
            categoryRepository.save(cat);
            ra.addFlashAttribute("successMessage", "Đã xóa loại sản phẩm [" + catName + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể xóa loại sản phẩm này: " + ex.getMessage());
        }
        return "redirect:/admin/categories";
    }

    /**
     * Alternative 6-9c: Khôi phục category (isDeleted = false)
     */
    @PostMapping("/categories/restore/{id}")
    public String restoreCategory(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            Category cat = categoryRepository.findById(id).orElse(null);
            if (cat == null) {
                ra.addFlashAttribute("errorMessage", "Không tìm thấy loại sản phẩm cần khôi phục!");
                return "redirect:/admin/categories";
            }
            String catName = cat.getName();
            cat.setIsDeleted(false);
            categoryRepository.save(cat);
            ra.addFlashAttribute("successMessage", "Khôi phục loại sản phẩm [" + catName + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể khôi phục loại sản phẩm này: " + ex.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/reset-defaults")
    public String resetDefaultCategories(RedirectAttributes ra) {
        record DefCat(String name, String desc) {}
        java.util.List<DefCat> defaults = java.util.List.of(
            new DefCat("Hoa Sinh Nhật", "Các mẫu bó hoa, lẵng hoa chúc mừng sinh nhật ngọt ngào, ý nghĩa"),
            new DefCat("Hoa Khai Trương", "Kệ hoa, lẵng hoa khai trương sang trọng phát tài phát lộc"),
            new DefCat("Hoa Tình Yêu & Valentine", "Bó hoa hồng đỏ Ecuador, hoa baby trắng lãng mạn tỏ tình"),
            new DefCat("Lan Hồ Điệp Sang Trọng", "Chậu hoa lan hồ điệp cao cấp trưng Tết, biếu sếp, mừng tân gia"),
            new DefCat("Hoa Cưới Cầm Tay Cô Dâu", "Bó hoa cưới tinh tế tôn vinh vẻ đẹp cô dâu trong ngày trọng đại"),
            new DefCat("Hoa Chúc Mừng & Tốt Nghiệp", "Thiết kế tươi sáng, gửi gắm lời chúc may mắn thành công"),
            new DefCat("Hoa Chia Buồn & Kính Viếng", "Kệ hoa trang trọng, thành kính phân ưu cùng gia quyến"),
            new DefCat("Bình Hoa Nghệ Thuật Để Bàn", "Bình gốm, bình thủy tinh cắm hoa tươi trang trí phòng khách và văn phòng"),
            new DefCat("Hộp Hoa & Giỏ Hoa Quà Tặng", "Hộp hoa mica, hộp tròn cao cấp và giỏ hoa tươi thiết kế độc đáo")
        );
        for (DefCat d : defaults) {
            java.util.Optional<Category> existing = categoryRepository.findByName(d.name());
            if (existing.isEmpty()) {
                categoryRepository.save(Category.builder().name(d.name()).description(d.desc()).isDeleted(false).build());
            } else if (Boolean.TRUE.equals(existing.get().getIsDeleted())) {
                Category c = existing.get();
                c.setIsDeleted(false);
                categoryRepository.save(c);
            }
        }
        ra.addFlashAttribute("successMessage", "Khôi phục toàn bộ danh sách 9 loại sản phẩm hoa tươi mẫu thành công!");
        return "redirect:/admin/categories";
    }

}

