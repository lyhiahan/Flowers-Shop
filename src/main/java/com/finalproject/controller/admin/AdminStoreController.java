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

        // Lọc theo tab (Tất cả, Đã cấp phép, Bị cấm)
        if ("APPROVED".equalsIgnoreCase(tab)) {
            stores = stores.stream().filter(s -> Boolean.TRUE.equals(s.getStatus())).toList();
        } else if ("BANNED".equalsIgnoreCase(tab)) {
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
        return "admin/stores";
    }

    /**
     * Cấp phép hoạt động kinh doanh cho cửa hàng trong hệ thống
     */
    @PostMapping("/approve/{id}")
    public String approveStore(@PathVariable("id") Long id, RedirectAttributes ra) {
        Store store = storeRepository.findById(id).orElseThrow();
        store.setStatus(true);
        storeRepository.save(store);
        ra.addFlashAttribute("successMessage", "Đã CẤP PHÉP hoạt động cho cửa hàng [" + store.getName() + "]. Cửa hàng hiện đã có thể mở bán và tiếp nhận đơn hàng trên hệ thống.");
        return "redirect:/admin/stores";
    }

    /**
     * Cấm hoặc đình chỉ hoạt động cửa hàng trong hệ thống
     */
    @PostMapping("/ban/{id}")
    public String banStore(@PathVariable("id") Long id, RedirectAttributes ra) {
        Store store = storeRepository.findById(id).orElseThrow();
        store.setStatus(false);
        storeRepository.save(store);
        ra.addFlashAttribute("successMessage", "Đã CẤM / THU HỒI giấy phép hoạt động của cửa hàng [" + store.getName() + "]. Cửa hàng sẽ bị ngừng kinh doanh trên toàn hệ thống.");
        return "redirect:/admin/stores";
    }

    /**
     * Bật/tắt trạng thái (tương thích ngược)
     */
    @PostMapping("/toggle-status/{id}")
    public String toggleStoreStatus(@PathVariable("id") Long id, RedirectAttributes ra) {
        Store store = storeRepository.findById(id).orElseThrow();
        boolean newStatus = !Boolean.TRUE.equals(store.getStatus());
        store.setStatus(newStatus);
        storeRepository.save(store);

        if (newStatus) {
            ra.addFlashAttribute("successMessage", "Đã cấp phép mở lại cho cửa hàng [" + store.getName() + "]. Cửa hàng đã có thể tiếp tục nhận đơn hàng.");
        } else {
            ra.addFlashAttribute("successMessage", "Đã cấm/tạm ngừng cửa hàng [" + store.getName() + "]. Cửa hàng sẽ không thể nhận đơn hàng mới.");
        }
        return "redirect:/admin/stores";
    }
}

