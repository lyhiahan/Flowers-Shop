package com.finalproject.controller.admin;

import com.finalproject.entity.Store;
import com.finalproject.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/stores")
@RequiredArgsConstructor
public class AdminStoreController {

    private final StoreRepository storeRepository;
    private final com.finalproject.repository.StoreLevelRepository storeLevelRepository;
    private final com.finalproject.repository.CommissionRepository commissionRepository;
    private final com.finalproject.repository.ProductRepository productRepository;

    @GetMapping
    public String listStores(@RequestParam(value = "keyword", required = false) String keyword,
                             @RequestParam(value = "tab", required = false, defaultValue = "ALL") String tab,
                             @RequestParam(value = "status", required = false) String statusStr,
                             Model model) {
        List<Store> allStores = storeRepository.findAll();

        long countAll = allStores.size();
        long countApproved = allStores.stream().filter(s -> Boolean.TRUE.equals(s.getStatus())).count();
        long countBanned = allStores.stream().filter(s -> !Boolean.TRUE.equals(s.getStatus())).count();

        List<Store> stores = allStores;

        // Lọc theo tab: ALL, LICENSED (APPROVED), UNLICENSED (BANNED) theo UC_018 Bước 2a1
        if ("APPROVED".equalsIgnoreCase(tab) || "LICENSED".equalsIgnoreCase(tab)) {
            stores = stores.stream().filter(s -> Boolean.TRUE.equals(s.getStatus())).toList();
        } else if ("BANNED".equalsIgnoreCase(tab) || "UNLICENSED".equalsIgnoreCase(tab)) {
            stores = stores.stream().filter(s -> !Boolean.TRUE.equals(s.getStatus())).toList();
        }

        // Lọc theo từ khóa
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            stores = stores.stream()
                    .filter(s -> (s.getName() != null && s.getName().toLowerCase().contains(kw))
                              || (s.getAddress() != null && s.getAddress().toLowerCase().contains(kw))
                              || (s.getPhone() != null && s.getPhone().contains(kw))
                              || (s.getOwner() != null && s.getOwner().getFullName() != null && s.getOwner().getFullName().toLowerCase().contains(kw)))
                    .toList();
        }

        // Lọc theo select status (nếu có dùng thêm)
        if (statusStr != null && !statusStr.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusStr)) {
            if ("ACTIVE".equalsIgnoreCase(statusStr) || "APPROVED".equalsIgnoreCase(statusStr)) {
                stores = stores.stream().filter(s -> Boolean.TRUE.equals(s.getStatus())).toList();
            } else if ("PAUSED".equalsIgnoreCase(statusStr) || "BANNED".equalsIgnoreCase(statusStr)) {
                stores = stores.stream().filter(s -> !Boolean.TRUE.equals(s.getStatus())).toList();
            }
        }

        model.addAttribute("stores", stores);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentTab", tab != null ? tab.toUpperCase() : "ALL");
        model.addAttribute("currentStatus", statusStr != null ? statusStr : "ALL");
        model.addAttribute("countAll", countAll);
        model.addAttribute("countApproved", countApproved);
        model.addAttribute("countBanned", countBanned);
        model.addAttribute("storeLevels", storeLevelRepository.findByIsDeletedFalseOrderByMinPointAsc());
        model.addAttribute("commissions", commissionRepository.findByIsDeletedFalse());
        return "admin/stores";
    }

    /**
     * Cập nhật Cấp bậc và Chính sách hoa hồng cho Chi nhánh
     */
    @PostMapping("/update-level/{id}")
    public String updateStoreLevel(@PathVariable("id") Long id,
                                   @RequestParam(value = "storeLevelId", required = false) Long storeLevelId,
                                   @RequestParam(value = "commissionId", required = false) Long commissionId,
                                   RedirectAttributes ra) {
        Store store = storeRepository.findById(id).orElseThrow();
        if (storeLevelId != null) {
            store.setStoreLevel(storeLevelRepository.findById(storeLevelId).orElse(null));
        }
        if (commissionId != null) {
            store.setCommission(commissionRepository.findById(commissionId).orElse(null));
        }
        storeRepository.save(store);
        ra.addFlashAttribute("successMessage", "Đã cập nhật cấp bậc và chính sách hoa hồng cho chi nhánh [" + store.getName() + "] thành công!");
        return "redirect:/admin/stores";
    }

    /**
     * 6a. Cấp phép (License) hoạt động kinh doanh cho cửa hàng trong hệ thống
     * 6a2. Cửa hàng và các sản phẩm của cửa hàng được cấp phép
     */
    @PostMapping("/approve/{id}")
    public String approveStore(@PathVariable("id") Long id, RedirectAttributes ra) {
        Store store = storeRepository.findById(id).orElseThrow();
        store.setStatus(true);
        storeRepository.save(store);

        // Kích hoạt lại toàn bộ sản phẩm của cửa hàng
        var products = productRepository.findByStoreIdOrderByCreatedAtDesc(id);
        if (products != null && !products.isEmpty()) {
            for (var p : products) {
                p.setIsActive(true);
                p.setIsSelling(true);
            }
            productRepository.saveAll(products);
        }

        ra.addFlashAttribute("successMessage", "Đã CẤP PHÉP (License) hoạt động cho cửa hàng [" + store.getName() + "]. Cửa hàng và tất cả sản phẩm đã được mở bán trên toàn hệ thống.");
        return "redirect:/admin/stores";
    }

    /**
     * 6a. Cấm (Ban) hoặc đình chỉ hoạt động cửa hàng trong hệ thống
     * 6a2. Cửa hàng và các sản phẩm của cửa hàng bị cấm
     */
    @PostMapping("/ban/{id}")
    public String banStore(@PathVariable("id") Long id, RedirectAttributes ra) {
        Store store = storeRepository.findById(id).orElseThrow();
        store.setStatus(false);
        storeRepository.save(store);

        // Vô hiệu hóa toàn bộ sản phẩm của cửa hàng
        var products = productRepository.findByStoreIdOrderByCreatedAtDesc(id);
        if (products != null && !products.isEmpty()) {
            for (var p : products) {
                p.setIsActive(false);
                p.setIsSelling(false);
            }
            productRepository.saveAll(products);
        }

        ra.addFlashAttribute("successMessage", "Đã CẤM (Ban) hoạt động đối với cửa hàng [" + store.getName() + "]. Cửa hàng và toàn bộ sản phẩm liên quan đã bị ngừng kinh doanh trên toàn hệ thống.");
        return "redirect:/admin/stores";
    }

    /**
     * Bật/tắt trạng thái (tương thích ngược và đồng bộ sản phẩm)
     */
    @PostMapping("/toggle-status/{id}")
    public String toggleStoreStatus(@PathVariable("id") Long id, RedirectAttributes ra) {
        Store store = storeRepository.findById(id).orElseThrow();
        boolean newStatus = !Boolean.TRUE.equals(store.getStatus());
        store.setStatus(newStatus);
        storeRepository.save(store);

        var products = productRepository.findByStoreIdOrderByCreatedAtDesc(id);
        if (products != null && !products.isEmpty()) {
            for (var p : products) {
                p.setIsActive(newStatus);
                p.setIsSelling(newStatus);
            }
            productRepository.saveAll(products);
        }

        if (newStatus) {
            ra.addFlashAttribute("successMessage", "Đã CẤP PHÉP (License) cho cửa hàng [" + store.getName() + "] và kích hoạt lại toàn bộ sản phẩm.");
        } else {
            ra.addFlashAttribute("successMessage", "Đã CẤM (Ban) cửa hàng [" + store.getName() + "]. Cửa hàng và toàn bộ sản phẩm đã bị ngừng hoạt động.");
        }
        return "redirect:/admin/stores";
    }
}

