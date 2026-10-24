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

@Controller
@RequestMapping({"/manager/stores", "/vendor/stores"})
@RequiredArgsConstructor
public class ManagerStoreController {

    private final StoreRepository storeRepository;
    private final com.finalproject.service.StoreCreationService creation;
    @InitBinder("store")
    public void bindStore(org.springframework.web.bind.WebDataBinder binder) {
        binder.setAllowedFields("name", "phone", "address", "bio", "isOpen");
    }
    private final UserRepository userRepository;
    private final com.finalproject.service.StoreAccessService storeAccess;
    private final com.finalproject.repository.CommissionRepository commissionRepository;

    @GetMapping
    public String listStores(@RequestParam(value = "page", defaultValue = "0") int page,
                             @RequestParam(value = "size", defaultValue = "6") int size,
                             Principal principal, Model model) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Store assigned = storeAccess.managedStore(user);
        var result = com.finalproject.utils.StorePages.load(page, size, pageable ->
                user.hasRole(com.finalproject.enums.Role.ADMIN) ? storeRepository.findAll(pageable)
                        : storeRepository.findAll((root, query, cb) -> cb.equal(root.get("id"), assigned == null ? -1L : assigned.getId()), pageable));
        model.addAttribute("stores", result.getContent());
        com.finalproject.utils.StorePages.model(model, result);
        model.addAttribute("currentUser", user);
        return "manager/stores";
    }

    /**
     * Bước 6 theo UC_018: Chọn nút Go to dashboard để đến trang Dashboard cửa hàng tương ứng
     */
    @PostMapping({"/switch/{id}", "/go-dashboard/{id}"})
    public String goToStoreDashboard(@PathVariable("id") Long id, Principal principal, RedirectAttributes ra) {
        userRepository.findByUsername(principal.getName()).orElseThrow();
        Store store = storeRepository.findById(id).orElseThrow();
        storeAccess.requireStore(store);

        // Chuyển sang chi nhánh tương ứng và lưu vào tài khoản
        storeAccess.select(store);

        ra.addFlashAttribute("successMessage", "Đã chuyển đến tổng quan cửa hàng: " + store.getName());
        return "redirect:/manager/dashboard";
    }

    @GetMapping({"/switch/{id}", "/go-dashboard/{id}"})
    public String legacyDashboard(@PathVariable Long id) {
        storeAccess.requireStore(storeRepository.findById(id).orElseThrow());
        return "redirect:/manager/dashboard?storeId=" + id;
    }

    @GetMapping("/create")
    public String showCreateStoreForm(Model model, RedirectAttributes ra) {
        if (!storeAccess.canCreateStore()) {
            ra.addFlashAttribute("errorMessage", "Mỗi manager chỉ được quản lý một cửa hàng. Bạn đã có cửa hàng phụ trách.");
            return "redirect:/manager/stores";
        }
        if (!model.containsAttribute("store")) model.addAttribute("store", new Store());
        model.addAttribute("requestId", java.util.UUID.randomUUID().toString());
        model.addAttribute("commissions", commissionRepository.findByIsDeletedFalse());
        return "manager/store-create";
    }

    @PostMapping("/create")
    public String createStore(Principal principal,
                              @ModelAttribute("store") Store store,
                              @RequestParam(value = "commissionId", required = false) Long commissionId,
                              @RequestParam(value="requestId", required=false) String requestId,
                              RedirectAttributes ra) {
        try {
            Store savedStore = creation.create(store, commissionId, requestId);
            storeAccess.select(savedStore);
            ra.addFlashAttribute("successMessage", "Tạo cửa hàng \"" + savedStore.getName() + "\" thành công!");
            // Bước 10 của UC_017: Cửa hàng mới được tạo, chuyển đến trang Dashboard cửa hàng
            return "redirect:/manager/dashboard";
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex instanceof IllegalArgumentException ? ex.getMessage() : "Không thể tạo cửa hàng. Vui lòng thử lại.");
            ra.addFlashAttribute("store", store);
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

        String name = formStore.getName() != null ? formStore.getName().trim() : "";
        if (name.isEmpty() || name.length() < 3 || name.length() > 120) {
            ra.addFlashAttribute("errorMessage", "Tên cửa hàng phải có độ dài từ 3 đến 120 ký tự!");
            return "redirect:/manager/stores/edit/" + id;
        }

        String rawPhone = formStore.getPhone() != null ? formStore.getPhone().trim() : "";
        if (rawPhone.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "Vui lòng nhập số điện thoại liên hệ của cửa hàng!");
            return "redirect:/manager/stores/edit/" + id;
        }
        String cleanPhone = rawPhone.replaceAll("[\\s.-]", "");
        if (!cleanPhone.matches("^(?:\\+84|84|0)(?:(?:3[2-9]|5[25689]|7[06-9]|8[1-9]|9[0-9])\\d{7}|2\\d{9})$")) {
            ra.addFlashAttribute("errorMessage", "Số điện thoại không đúng chuẩn Việt Nam! Vui lòng nhập số hợp lệ (ví dụ: 0901234567 hoặc 02838123456).");
            return "redirect:/manager/stores/edit/" + id;
        }

        String address = formStore.getAddress() != null ? formStore.getAddress().trim() : "";
        if (address.isEmpty() || address.length() < 5 || address.length() > 200) {
            ra.addFlashAttribute("errorMessage", "Địa chỉ cửa hàng phải có ít nhất 5 ký tự và tối đa 200 ký tự!");
            return "redirect:/manager/stores/edit/" + id;
        }

        if (formStore.getBio() != null && formStore.getBio().length() > 500) {
            ra.addFlashAttribute("errorMessage", "Bio tối đa 500 ký tự.");
            return "redirect:/manager/stores/edit/" + id;
        }
        store.setName(name);
        store.setPhone(cleanPhone);
        store.setAddress(address);
        if (formStore.getBio() != null) store.setBio(formStore.getBio().trim());
        if (formStore.getIsOpen() != null) store.setIsOpen(formStore.getIsOpen());

        storeRepository.save(store);
        ra.addFlashAttribute("successMessage", "Cập nhật thông tin cửa hàng thành công!");
        return "redirect:/manager/stores";
    }
}
