package com.finalproject.controller.admin;

import com.finalproject.entity.Store;
import com.finalproject.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/stores")
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional
public class AdminStoreController {

    private final StoreRepository storeRepository;
    private final com.finalproject.repository.StoreLevelRepository storeLevelRepository;
    private final com.finalproject.repository.CommissionRepository commissionRepository;

    @GetMapping
    public String listStores(@RequestParam(value = "keyword", required = false) String keyword,
                             @RequestParam(value = "tab", required = false, defaultValue = "ALL") String tab,
                             @RequestParam(value = "status", required = false) String statusStr,
                             @RequestParam(value="page", defaultValue="0") int page,
                             @RequestParam(value="size", defaultValue="8") int size, Model model) {
        Boolean status = null;
        if (tab != null && !tab.equalsIgnoreCase("ALL")) {
            if ("LICENSED".equalsIgnoreCase(tab) || "APPROVED".equalsIgnoreCase(tab)) {
                status = true;
            } else if ("UNLICENSED".equalsIgnoreCase(tab) || "BANNED".equalsIgnoreCase(tab)) {
                status = false;
            }
        }
        if (status == null && statusStr != null && !statusStr.equalsIgnoreCase("ALL")) {
            if ("ACTIVE".equalsIgnoreCase(statusStr) || "APPROVED".equalsIgnoreCase(statusStr)) {
                status = true;
            } else if ("PAUSED".equalsIgnoreCase(statusStr) || "BANNED".equalsIgnoreCase(statusStr)) {
                status = false;
            }
        }
        final Boolean filterStatus = status;
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        var result = com.finalproject.utils.StorePages.load(page, size, pageable -> storeRepository.findAll((root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (filterStatus != null) predicates.add(cb.equal(root.get("status"), filterStatus));
            if (!kw.isEmpty()) predicates.add(cb.or(cb.like(cb.lower(root.get("name")), "%" + kw + "%"), cb.like(cb.lower(root.get("address")), "%" + kw + "%"), cb.like(root.get("phone"), "%" + kw + "%"), cb.like(cb.lower(root.join("owner", jakarta.persistence.criteria.JoinType.LEFT).get("fullName")), "%" + kw + "%")));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, pageable));
        model.addAttribute("stores", result.getContent());
        com.finalproject.utils.StorePages.model(model, result);
        long countAll = storeRepository.count();
        long countApproved = storeRepository.countByStatus(true);
        long countBanned = countAll - countApproved;
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
        Store store = storeRepository.findLockedById(id).orElseThrow();
        if (storeLevelId != null) {
            store.setStoreLevel(storeLevelRepository.findById(storeLevelId).filter(l -> !Boolean.TRUE.equals(l.getIsDeleted())).orElseThrow(() -> new IllegalArgumentException("Cấp cửa hàng không hợp lệ.")));
        }
        if (commissionId != null) {
            store.setCommission(commissionRepository.findById(commissionId).filter(c -> !Boolean.TRUE.equals(c.getIsDeleted())).orElseThrow(() -> new IllegalArgumentException("Chính sách hoa hồng không hợp lệ.")));
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
        Store store = storeRepository.findLockedById(id).orElseThrow();
        store.setStatus(true);
        storeRepository.save(store);

        ra.addFlashAttribute("successMessage", "Đã CẤP PHÉP (License) hoạt động cho cửa hàng [" + store.getName() + "]. Cửa hàng và tất cả sản phẩm đã được mở bán trên toàn hệ thống.");
        return "redirect:/admin/stores";
    }

    /**
     * 6a. Cấm (Ban) hoặc đình chỉ hoạt động cửa hàng trong hệ thống
     * 6a2. Cửa hàng và các sản phẩm của cửa hàng bị cấm
     */
    @PostMapping("/ban/{id}")
    public String banStore(@PathVariable("id") Long id, RedirectAttributes ra) {
        Store store = storeRepository.findLockedById(id).orElseThrow();
        store.setStatus(false);
        storeRepository.save(store);

        ra.addFlashAttribute("successMessage", "Đã CẤM (Ban) hoạt động đối với cửa hàng [" + store.getName() + "]. Cửa hàng và toàn bộ sản phẩm liên quan đã bị ngừng kinh doanh trên toàn hệ thống.");
        return "redirect:/admin/stores";
    }

    /**
     * Bật/tắt trạng thái (tương thích ngược và đồng bộ sản phẩm)
     */
    @PostMapping("/toggle-status/{id}")
    public String toggleStoreStatus(@PathVariable("id") Long id, RedirectAttributes ra) {
        Store store = storeRepository.findLockedById(id).orElseThrow();
        boolean newStatus = !Boolean.TRUE.equals(store.getStatus());
        store.setStatus(newStatus);
        storeRepository.save(store);

        if (newStatus) {
            ra.addFlashAttribute("successMessage", "Đã CẤP PHÉP (License) cho cửa hàng [" + store.getName() + "] và kích hoạt lại toàn bộ sản phẩm.");
        } else {
            ra.addFlashAttribute("successMessage", "Đã CẤM (Ban) cửa hàng [" + store.getName() + "]. Cửa hàng và toàn bộ sản phẩm đã bị ngừng hoạt động.");
        }
        return "redirect:/admin/stores";
    }
}

