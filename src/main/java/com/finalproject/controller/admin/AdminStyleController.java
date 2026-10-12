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
public class AdminStyleController {
    private final CategoryRepository categoryRepository;
    private final StyleRepository styleRepository;
    private final StyleValueRepository styleValueRepository;

    // --- 7. Quản lý Thuộc tính sản phẩm (Style - UC_031) ---
    /**
     * Bước 1 - 4: Hiển thị danh sách style, phân trang Next/Prev
     */
    @GetMapping({"/styles", "/style", "/thuoc-tinh-san-pham"})
    public String listStyles(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {
        try {
            java.util.List<Style> allStyles = styleRepository.findAll();

            java.util.List<Style> filtered = allStyles;
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = keyword.trim().toLowerCase();
                filtered = filtered.stream().filter(s ->
                        (s.getName() != null && s.getName().toLowerCase().contains(kw))
                        || (s.getCategory() != null && s.getCategory().getName() != null && s.getCategory().getName().toLowerCase().contains(kw))
                        || String.valueOf(s.getId()).contains(kw)
                ).toList();
            }

            int totalElements = filtered.size();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            if (totalPages == 0) totalPages = 1;
            int fromIndex = Math.min(page * size, totalElements);
            int toIndex = Math.min(fromIndex + size, totalElements);
            java.util.List<Style> pagedStyles = filtered.subList(fromIndex, toIndex);

            model.addAttribute("styles", pagedStyles);
            model.addAttribute("allStyles", allStyles);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalElements", (long) totalElements);
            model.addAttribute("pageSize", size);
            model.addAttribute("keyword", keyword);
            model.addAttribute("dataLoadError", false);
        } catch (Exception ex) {
            model.addAttribute("dataLoadError", true);
            model.addAttribute("errorMessage", "Không thể tải danh sách thuộc tính sản phẩm. Vui lòng thử lại!");
            model.addAttribute("styles", java.util.Collections.emptyList());
            model.addAttribute("allStyles", java.util.Collections.emptyList());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalElements", 0L);
            model.addAttribute("pageSize", size);
        }
        return "admin/styles";
    }

    /**
     * Bước 5 & 6: Chuyển đến form tạo style mới
     */
    @GetMapping("/styles/create")
    public String createStyleForm(Model model) {
        model.addAttribute("style", new Style());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("isEdit", false);
        return "admin/style-form";
    }

    /**
     * Alternative 5a & 6a: Chuyển đến form chỉnh sửa style
     */
    @GetMapping("/styles/edit/{id}")
    public String editStyleForm(@PathVariable("id") Long id, Model model) {
        Style s = styleRepository.findById(id).orElse(null);
        if (s == null) {
            return "redirect:/admin/styles";
        }
        model.addAttribute("style", s);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("isEdit", true);
        return "admin/style-form";
    }

    /**
     * Bước 7 - 11: Thêm mới style
     */
    @PostMapping("/styles/add")
    public String addStyle(@RequestParam("name") String name,
                           @RequestParam(value = "categoryId", required = false) Long categoryId,
                           RedirectAttributes ra) {
        try {
            Category cat = null;
            if (categoryId != null && categoryId > 0) {
                cat = categoryRepository.findById(categoryId).orElse(null);
            }
            Style s = Style.builder()
                    .name(name)
                    .category(cat)
                    .isDeleted(false)
                    .build();
            styleRepository.save(s);
            ra.addFlashAttribute("successMessage", "Thêm mới thuộc tính [" + name + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể thêm thuộc tính: " + ex.getMessage());
        }
        return "redirect:/admin/styles";
    }

    /**
     * Alternative 6a: Cập nhật style
     */
    @PostMapping("/styles/update/{id}")
    public String updateStyle(@PathVariable("id") Long id,
                              @RequestParam("name") String name,
                              @RequestParam(value = "categoryId", required = false) Long categoryId,
                              RedirectAttributes ra) {
        try {
            Style s = styleRepository.findById(id).orElseThrow();
            s.setName(name);
            if (categoryId != null && categoryId > 0) {
                s.setCategory(categoryRepository.findById(categoryId).orElse(null));
            } else if (categoryId != null && categoryId == 0) {
                s.setCategory(null);
            }
            styleRepository.save(s);
            ra.addFlashAttribute("successMessage", "Cập nhật thuộc tính [" + name + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể cập nhật thuộc tính: " + ex.getMessage());
        }
        return "redirect:/admin/styles";
    }

    /**
     * Alternative 5-9b: Xóa style (isDeleted = true)
     */
    @PostMapping("/styles/delete/{id}")
    public String deleteStyle(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            Style s = styleRepository.findById(id).orElseThrow();
            s.setIsDeleted(true);
            styleRepository.save(s);
            ra.addFlashAttribute("successMessage", "Đã xóa thuộc tính [" + s.getName() + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể xóa thuộc tính: " + ex.getMessage());
        }
        return "redirect:/admin/styles";
    }

    /**
     * Alternative 5-9c: Khôi phục style (isDeleted = false)
     */
    @PostMapping("/styles/restore/{id}")
    public String restoreStyle(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            Style s = styleRepository.findById(id).orElseThrow();
            s.setIsDeleted(false);
            styleRepository.save(s);
            ra.addFlashAttribute("successMessage", "Khôi phục thuộc tính [" + s.getName() + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể khôi phục thuộc tính: " + ex.getMessage());
        }
        return "redirect:/admin/styles";
    }

    /**
     * Alternative 5d & 6d1: Chọn nút Detail để xem danh sách các giá trị của thuộc tính (style value)
     */
    @GetMapping("/styles/{id}/values")
    public String listStyleValues(@PathVariable("id") Long id,
                                  @RequestParam(value = "keyword", required = false) String keyword,
                                  @RequestParam(value = "page", defaultValue = "0") int page,
                                  @RequestParam(value = "size", defaultValue = "10") int size,
                                  Model model) {
        try {
            Style style = styleRepository.findById(id).orElse(null);
            if (style == null) {
                return "redirect:/admin/styles";
            }
            java.util.List<StyleValue> allValues = styleValueRepository.findByStyleId(id);
            java.util.List<StyleValue> filtered = allValues;
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = keyword.trim().toLowerCase();
                filtered = filtered.stream()
                        .filter(v -> v.getName() != null && v.getName().toLowerCase().contains(kw))
                        .toList();
            }

            int totalElements = filtered.size();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            if (totalPages == 0) totalPages = 1;
            int fromIndex = Math.min(page * size, totalElements);
            int toIndex = Math.min(fromIndex + size, totalElements);
            java.util.List<StyleValue> pagedValues = filtered.subList(fromIndex, toIndex);

            model.addAttribute("style", style);
            model.addAttribute("styleValues", pagedValues);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalElements", (long) totalElements);
            model.addAttribute("pageSize", size);
            model.addAttribute("keyword", keyword);
            model.addAttribute("dataLoadError", false);
        } catch (Exception ex) {
            model.addAttribute("dataLoadError", true);
            model.addAttribute("errorMessage", "Không thể tải danh sách giá trị thuộc tính. Vui lòng thử lại!");
            model.addAttribute("styleValues", java.util.Collections.emptyList());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalElements", 0L);
            model.addAttribute("pageSize", size);
        }
        return "admin/style-values";
    }

    /**
     * Alternative 6d2 & 6d3: Thêm mới style value
     */
    @PostMapping("/styles/{styleId}/values/add")
    public String addStyleValue(@PathVariable("styleId") Long styleId,
                                @RequestParam("name") String name,
                                RedirectAttributes ra) {
        try {
            Style style = styleRepository.findById(styleId).orElseThrow();
            StyleValue val = StyleValue.builder()
                    .name(name)
                    .style(style)
                    .isDeleted(false)
                    .build();
            styleValueRepository.save(val);
            ra.addFlashAttribute("successMessage", "Thêm mới giá trị thuộc tính [" + name + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể thêm giá trị thuộc tính: " + ex.getMessage());
        }
        return "redirect:/admin/styles/" + styleId + "/values";
    }

    /**
     * Alternative 6e2 & 6e3: Sửa style value
     */
    @PostMapping("/styles/{styleId}/values/update/{valueId}")
    public String updateStyleValue(@PathVariable("styleId") Long styleId,
                                  @PathVariable("valueId") Long valueId,
                                  @RequestParam("name") String name,
                                  RedirectAttributes ra) {
        try {
            StyleValue val = styleValueRepository.findById(valueId).orElseThrow();
            val.setName(name);
            styleValueRepository.save(val);
            ra.addFlashAttribute("successMessage", "Cập nhật giá trị thuộc tính [" + name + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể cập nhật giá trị thuộc tính: " + ex.getMessage());
        }
        return "redirect:/admin/styles/" + styleId + "/values";
    }

    /**
     * Alternative 6f2-9: Xóa style value (isDeleted = true)
     */
    @PostMapping("/styles/{styleId}/values/delete/{valueId}")
    public String deleteStyleValue(@PathVariable("styleId") Long styleId,
                                  @PathVariable("valueId") Long valueId,
                                  RedirectAttributes ra) {
        try {
            StyleValue val = styleValueRepository.findById(valueId).orElseThrow();
            val.setIsDeleted(true);
            styleValueRepository.save(val);
            ra.addFlashAttribute("successMessage", "Đã xóa giá trị thuộc tính [" + val.getName() + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể xóa giá trị thuộc tính: " + ex.getMessage());
        }
        return "redirect:/admin/styles/" + styleId + "/values";
    }

    /**
     * Alternative 6g2-9: Khôi phục style value (isDeleted = false)
     */
    @PostMapping("/styles/{styleId}/values/restore/{valueId}")
    public String restoreStyleValue(@PathVariable("styleId") Long styleId,
                                   @PathVariable("valueId") Long valueId,
                                   RedirectAttributes ra) {
        try {
            StyleValue val = styleValueRepository.findById(valueId).orElseThrow();
            val.setIsDeleted(false);
            styleValueRepository.save(val);
            ra.addFlashAttribute("successMessage", "Khôi phục giá trị thuộc tính [" + val.getName() + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể khôi phục giá trị thuộc tính: " + ex.getMessage());
        }
        return "redirect:/admin/styles/" + styleId + "/values";
    }

    /**
     * Xóa vĩnh viễn Style Value khỏi hệ thống (Hard Delete)
     */
    @PostMapping("/styles/{styleId}/values/destroy/{valueId}")
    public String destroyStyleValue(@PathVariable("styleId") Long styleId,
                                    @PathVariable("valueId") Long valueId,
                                    RedirectAttributes ra) {
        try {
            StyleValue val = styleValueRepository.findById(valueId).orElseThrow();
            String name = val.getName();
            styleValueRepository.delete(val);
            ra.addFlashAttribute("successMessage", "Đã xóa hoàn toàn giá trị thuộc tính [" + name + "] khỏi hệ thống!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể xóa giá trị thuộc tính: " + ex.getMessage());
        }
        return "redirect:/admin/styles/" + styleId + "/values";
    }

    /**
     * Bổ sung thuộc tính mặc định: Đồng bộ an toàn 4 nhóm thuộc tính cốt lõi và các giá trị chuẩn
     * Tuyệt đối không xóa hay ghi đè dữ liệu người dùng đã tạo
     */
    @PostMapping({"/styles/seed-defaults", "/styles/reset-defaults"})
    public String seedDefaultStyles(RedirectAttributes ra) {
        try {
            // 1. Chuẩn hóa tên cũ sang tên mới thống nhất
            styleRepository.findAll().forEach(s -> {
                String n = s.getName().trim();
                if (n.equalsIgnoreCase("Tone màu hoa") || n.equalsIgnoreCase("Tone màu thiết kế")) {
                    s.setName("Màu sắc");
                    s.setIsDeleted(false);
                    styleRepository.save(s);
                } else if (n.equalsIgnoreCase("Kiểu dáng cắm") || n.equalsIgnoreCase("Kiểu dáng hoa")) {
                    s.setName("Kiểu dáng");
                    s.setIsDeleted(false);
                    styleRepository.save(s);
                } else if (n.equalsIgnoreCase("Kích cỡ bó hoa") || n.equalsIgnoreCase("Kích cỡ thiết kế")) {
                    s.setName("Kích cỡ");
                    s.setIsDeleted(false);
                    styleRepository.save(s);
                }
            });

            // 2. Định nghĩa 4 nhóm chính và các nhóm bổ sung cùng giá trị mẫu
            java.util.Map<String, java.util.List<String>> defaultGroups = new java.util.LinkedHashMap<>();
            defaultGroups.put("Loại hoa", java.util.List.of(
                    "Hướng dương", "Hoa hồng", "Tulip", "Cẩm tú cầu", "Lan hồ điệp", "Calla Lily", "Hoa Baby"
            ));
            defaultGroups.put("Kiểu dáng", java.util.List.of(
                    "Bó hoa", "Giỏ hoa", "Bình hoa", "Hộp hoa", "Chậu hoa", "Kệ hoa"
            ));
            defaultGroups.put("Màu sắc", java.util.List.of(
                    "Vàng", "Hồng", "Trắng", "Đỏ", "Tím", "Xanh", "Cam"
            ));
            defaultGroups.put("Dịp tặng", java.util.List.of(
                    "Sinh nhật", "Tốt nghiệp", "Khai trương", "Tình yêu", "Chúc mừng", "Đám cưới", "Chia buồn"
            ));
            defaultGroups.put("Phong cách thiết kế", java.util.List.of(
                    "Hàn Quốc hiện đại", "Vintage cổ điển", "Tối giản Minimalist", "Châu Âu sang trọng"
            ));
            defaultGroups.put("Kích cỡ", java.util.List.of(
                    "Size S (Nhỏ)", "Size M (Vừa)", "Size L (Lớn)"
            ));

            int addedGroups = 0;
            int addedValues = 0;

            for (java.util.Map.Entry<String, java.util.List<String>> entry : defaultGroups.entrySet()) {
                String groupName = entry.getKey();
                java.util.List<String> values = entry.getValue();

                java.util.Optional<Style> existingStyleOpt = styleRepository.findAll().stream()
                        .filter(s -> s.getName().equalsIgnoreCase(groupName))
                        .findFirst();

                Style style;
                if (existingStyleOpt.isPresent()) {
                    style = existingStyleOpt.get();
                    if (Boolean.TRUE.equals(style.getIsDeleted())) {
                        style.setIsDeleted(false);
                        styleRepository.save(style);
                    }
                } else {
                    style = styleRepository.save(Style.builder()
                            .name(groupName)
                            .isDeleted(false)
                            .build());
                    addedGroups++;
                }

                java.util.List<StyleValue> existingValues = styleValueRepository.findByStyleId(style.getId());
                java.util.Set<String> existingNames = existingValues.stream()
                        .map(v -> v.getName().toLowerCase().trim())
                        .collect(java.util.stream.Collectors.toSet());

                for (String valName : values) {
                    if (!existingNames.contains(valName.toLowerCase().trim())) {
                        styleValueRepository.save(StyleValue.builder()
                                .name(valName)
                                .style(style)
                                .isDeleted(false)
                                .build());
                        addedValues++;
                    }
                }
            }

            ra.addFlashAttribute("successMessage", "Đã bổ sung thuộc tính mặc định thành công! (" + addedGroups + " nhóm mới, " + addedValues + " giá trị mới). Toàn bộ dữ liệu bạn đã tạo vẫn được giữ nguyên an toàn.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể bổ sung thuộc tính: " + ex.getMessage());
        }
        return "redirect:/admin/styles";
    }
}

