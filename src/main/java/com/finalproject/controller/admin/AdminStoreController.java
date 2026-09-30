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
                             @RequestParam(value = "status", required = false) String statusStr,
                             Model model) {
        List<Store> stores = storeRepository.findAll();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            stores = stores.stream()
                    .filter(s -> (s.getName() != null && s.getName().toLowerCase().contains(kw))
                              || (s.getAddress() != null && s.getAddress().toLowerCase().contains(kw))
                              || (s.getPhone() != null && s.getPhone().contains(kw))
                              || (s.getOwner() != null && s.getOwner().getFullName() != null && s.getOwner().getFullName().toLowerCase().contains(kw)))
                    .toList();
        }

        if (statusStr != null && !statusStr.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusStr)) {
            if ("ACTIVE".equalsIgnoreCase(statusStr)) {
                stores = stores.stream().filter(s -> Boolean.TRUE.equals(s.getStatus())).toList();
            } else if ("PAUSED".equalsIgnoreCase(statusStr)) {
                stores = stores.stream().filter(s -> !Boolean.TRUE.equals(s.getStatus())).toList();
            }
        }

        model.addAttribute("stores", stores);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentStatus", statusStr != null ? statusStr : "ALL");
        return "admin/stores";
    }

    @PostMapping("/toggle-status/{id}")
    public String toggleStoreStatus(@PathVariable("id") Long id, RedirectAttributes ra) {
        Store store = storeRepository.findById(id).orElseThrow();
        boolean newStatus = !Boolean.TRUE.equals(store.getStatus());
        store.setStatus(newStatus);
        storeRepository.save(store);

        if (newStatus) {
            ra.addFlashAttribute("successMessage", "Đã mở lại hoạt động cho chi nhánh [" + store.getName() + "]. Chi nhánh đã có thể tiếp tục nhận đơn hàng.");
        } else {
            ra.addFlashAttribute("successMessage", "Đã chuyển chi nhánh [" + store.getName() + "] sang trạng thái [Tạm ngừng]. Chi nhánh sẽ không nhận đơn mới; các đơn đang xử lý cần được hoàn tất.");
        }
        return "redirect:/admin/stores";
    }
}

