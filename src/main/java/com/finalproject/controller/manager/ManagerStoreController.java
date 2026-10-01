package com.finalproject.controller.manager;

import com.finalproject.entity.Store;
import com.finalproject.entity.User;
import com.finalproject.repository.StoreRepository;
import com.finalproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping({"/manager/stores", "/vendor/stores"})
@RequiredArgsConstructor
public class ManagerStoreController {

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final com.finalproject.service.StoreAccessService storeAccess;
    private final com.finalproject.repository.CommissionRepository commissionRepository;
    private final com.finalproject.repository.StoreLevelRepository storeLevelRepository;

    @GetMapping
    public String listStores(@RequestParam(value = "page", defaultValue = "0") int page,
                             @RequestParam(value = "size", defaultValue = "6") int size,
                             Principal principal, Model model) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        List<Store> ownedStores = storeRepository.findByOwnerId(user.getId());
        java.util.Set<Store> allStoresSet = new java.util.LinkedHashSet<>(ownedStores);
        if (user.getStore() != null) {
            allStoresSet.add(user.getStore());
        }
        List<Store> allStores = new java.util.ArrayList<>(allStoresSet);

        // Phân trang danh sách cửa hàng (Bước 5 theo UC_018)
        int totalElements = allStores.size();
        int totalPages = totalElements > 0 ? (int) Math.ceil((double) totalElements / size) : 1;
        if (page < 0) page = 0;
        if (page >= totalPages && totalElements > 0) page = totalPages - 1;

        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<Store> pagedStores = allStores.subList(fromIndex, toIndex);

        model.addAttribute("stores", pagedStores);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalElements", totalElements);
        model.addAttribute("pageSize", size);
        model.addAttribute("currentUser", user);
        return "manager/stores";
    }

    /**
     * Bước 6 theo UC_018: Chọn nút Go to dashboard để đến trang Dashboard cửa hàng tương ứng
     */
    @GetMapping({"/switch/{id}", "/go-dashboard/{id}"})
    public String goToStoreDashboard(@PathVariable("id") Long id, Principal principal, RedirectAttributes ra) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Store store = storeRepository.findById(id).orElseThrow();
        storeAccess.requireStore(store);

        // Chuyển sang chi nhánh tương ứng và lưu vào tài khoản
        user.setStore(store);
        userRepository.save(user);

        ra.addFlashAttribute("successMessage", "Đã chuyển đến tổng quan cửa hàng: " + store.getName());
        return "redirect:/manager/dashboard";
    }

    @GetMapping("/create")
    public String showCreateStoreForm(Model model) {
        model.addAttribute("store", new Store());
        model.addAttribute("commissions", commissionRepository.findByIsDeletedFalse());
        return "manager/store-create";
    }

    @PostMapping("/create")
    public String createStore(Principal principal,
                              @ModelAttribute("store") Store store,
                              @RequestParam(value = "commissionId", required = false) Long commissionId,
                              RedirectAttributes ra) {
        try {
            User user = userRepository.findByUsername(principal.getName()).orElseThrow();

            if (store.getName() == null || store.getName().trim().isEmpty()) {
                ra.addFlashAttribute("errorMessage", "Vui lòng điền tên chi nhánh cửa hàng hoa!");
                return "redirect:/manager/stores/create";
            }
            if (store.getPhone() == null || store.getPhone().trim().isEmpty()) {
                ra.addFlashAttribute("errorMessage", "Vui lòng điền số điện thoại hotline chi nhánh!");
                return "redirect:/manager/stores/create";
            }
            if (store.getAddress() == null || store.getAddress().trim().isEmpty()) {
                ra.addFlashAttribute("errorMessage", "Vui lòng điền địa chỉ chi nhánh chi tiết!");
                return "redirect:/manager/stores/create";
            }

            store.setId(null);
            store.setCreatedAt(java.time.LocalDateTime.now());
            store.setEWallet(java.math.BigDecimal.ZERO);

            // Gán gói Commission đã chọn
            if (commissionId != null) {
                store.setCommission(commissionRepository.findById(commissionId).orElse(null));
            } else {
                // Nếu không chọn cụ thể, gán gói cơ bản đầu tiên
                var defaultComm = commissionRepository.findByIsDeletedFalse().stream().findFirst().orElse(null);
                store.setCommission(defaultComm);
            }

            // Gán hạng chi nhánh mặc định cơ sở
            var defaultLevel = storeLevelRepository.findAll().stream()
                    .filter(sl -> sl.getMinPoint() != null && sl.getMinPoint() == 0)
                    .findFirst()
                    .orElse(null);
            store.setStoreLevel(defaultLevel);

            store.setPoint(0);
            store.setOwner(user);
            store.setStatus(true);
            store.setIsOpen(true);
            store.setRating(5.0);
            Store savedStore = storeRepository.save(store);

            // Gán chi nhánh làm việc hiện tại cho user sang cửa hàng vừa tạo
            user.setStore(savedStore);
            if (user.getRole() == com.finalproject.enums.Role.CUSTOMER) {
                user.setRole(com.finalproject.enums.Role.VENDOR);
            }
            userRepository.save(user);

            ra.addFlashAttribute("successMessage", "Tạo cửa hàng [" + savedStore.getName() + "] thành công!");
            // Bước 10 của UC_017: Cửa hàng mới được tạo, chuyển đến trang Dashboard cửa hàng
            return "redirect:/manager/dashboard";
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Tạo cửa hàng thất bại: " + ex.getMessage());
            return "redirect:/manager/stores/create";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditStoreForm(@PathVariable("id") Long id, Model model) {
        Store store = storeRepository.findById(id).orElseThrow();
        storeAccess.requireManager(store);
        model.addAttribute("store", store);
        return "manager/store-info";
    }

    @PostMapping("/edit/{id}")
    public String updateStore(@PathVariable("id") Long id,
                              @ModelAttribute("store") Store formStore,
                              RedirectAttributes ra) {
        Store store = storeRepository.findById(id).orElseThrow();
        storeAccess.requireManager(store);
        store.setName(formStore.getName());
        store.setPhone(formStore.getPhone());
        store.setAddress(formStore.getAddress());
        store.setBio(formStore.getBio());
        store.setAvatar(formStore.getAvatar());
        store.setCover(formStore.getCover());
        store.setFeaturedImages(formStore.getFeaturedImages());
        store.setIsOpen(formStore.getIsOpen());

        storeRepository.save(store);
        ra.addFlashAttribute("successMessage", "Cập nhật thông tin cửa hàng thành công!");
        return "redirect:/manager/stores";
    }
}
