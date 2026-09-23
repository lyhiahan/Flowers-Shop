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
    public String listStores(Model model) {
        List<Store> stores = storeRepository.findAll();
        model.addAttribute("stores", stores);
        return "admin/stores";
    }

    @PostMapping("/toggle-status/{id}")
    public String toggleStoreStatus(@PathVariable("id") Long id, RedirectAttributes ra) {
        Store store = storeRepository.findById(id).orElseThrow();
        store.setStatus(!Boolean.TRUE.equals(store.getStatus()));
        storeRepository.save(store);
        ra.addFlashAttribute("successMessage", "Đã cập nhật giấy phép hoạt động chi nhánh [" + store.getName() + "] thành [" + (store.getStatus() ? "Đã cấp phép" : "Tạm ngưng / Cấm") + "]");
        return "redirect:/admin/stores";
    }
}
