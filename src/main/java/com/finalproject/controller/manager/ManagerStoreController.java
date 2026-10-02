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

            String name = store.getName() != null ? store.getName().trim() : "";
            if (name.isEmpty()) {
                ra.addFlashAttribute("errorMessage", "Vui lòng nhập tên cửa hàng!");
                return "redirect:/manager/stores/create";
            }
            if (name.length() < 3 || name.length() > 120) {
                ra.addFlashAttribute("errorMessage", "Tên cửa hàng phải có độ dài từ 3 đến 120 ký tự!");
                return "redirect:/manager/stores/create";
            }

            String rawPhone = store.getPhone() != null ? store.getPhone().trim() : "";
            if (rawPhone.isEmpty()) {
                ra.addFlashAttribute("errorMessage", "Vui lòng nhập số điện thoại liên hệ của cửa hàng!");
                return "redirect:/manager/stores/create";
            }
            String cleanPhone = rawPhone.replaceAll("[\\s.-]", "");
            // Kiểm tra định dạng số điện thoại chuẩn Việt Nam:
            // Di động 10 số (đầu 03, 05, 07, 08, 09) hoặc máy bàn 11 số (đầu 02x), hỗ trợ tiền tố +84 hoặc 84
            if (!cleanPhone.matches("^(?:\\+84|84|0)(?:(?:3[2-9]|5[25689]|7[06-9]|8[1-9]|9[0-9])\\d{7}|2\\d{9})$")) {
                ra.addFlashAttribute("errorMessage", "Số điện thoại không đúng chuẩn Việt Nam! Vui lòng nhập số hợp lệ (ví dụ: 0901234567, 0389998888 hoặc máy bàn 02838123456).");
                return "redirect:/manager/stores/create";
            }
            store.setPhone(cleanPhone);

            String address = store.getAddress() != null ? store.getAddress().trim() : "";
            if (address.isEmpty()) {
                ra.addFlashAttribute("errorMessage", "Vui lòng nhập địa chỉ cụ thể của cửa hàng!");
                return "redirect:/manager/stores/create";
            }
            if (address.length() < 5 || address.length() > 200) {
                ra.addFlashAttribute("errorMessage", "Địa chỉ cửa hàng phải có ít nhất 5 ký tự và tối đa 200 ký tự!");
                return "redirect:/manager/stores/create";
            }

            if (store.getBio() != null && store.getBio().trim().length() > 500) {
                ra.addFlashAttribute("errorMessage", "Mô tả giới thiệu cửa hàng không được vượt quá 500 ký tự!");
                return "redirect:/manager/stores/create";
            }

            store.setName(name);
            store.setAddress(address);
            store.setId(null);
            store.setCreatedAt(java.time.LocalDateTime.now());
            store.setEWallet(java.math.BigDecimal.ZERO);

            // Bước 6 của UC_017: Xử lý loại hình commission
            if (commissionId != null) {
                commissionRepository.findById(commissionId).ifPresent(store::setCommission);
            }
            if (store.getCommission() == null) {
                var defaultCommissions = commissionRepository.findByIsDeletedFalse();
                if (!defaultCommissions.isEmpty()) {
                    store.setCommission(defaultCommissions.get(0));
                }
            }

            // Gán hạng cửa hàng mặc định
            var defaultLevel = storeLevelRepository.findAll().stream()
                    .filter(sl -> sl.getMinPoint() != null && sl.getMinPoint() == 0)
                    .findFirst()
                    .orElse(null);
            store.setStoreLevel(defaultLevel);

            store.setPoint(0);
            store.setOwner(user);
            store.setStatus(true);
            if (store.getIsOpen() == null) {
                store.setIsOpen(true);
            }
            store.setRating(0.0); // Khởi tạo 0.0 (chưa có đánh giá thực tế)
            Store savedStore = storeRepository.save(store);

            // Gán cửa hàng làm việc hiện tại cho user sang cửa hàng vừa tạo
            user.setStore(savedStore);
            if (user.getRole() == com.finalproject.enums.Role.CUSTOMER) {
                user.setRole(com.finalproject.enums.Role.VENDOR);
            }
            userRepository.save(user);

            ra.addFlashAttribute("successMessage", "Tạo cửa hàng \"" + savedStore.getName() + "\" thành công!");
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

        store.setName(name);
        store.setPhone(cleanPhone);
        store.setAddress(address);
        store.setBio(formStore.getBio() != null ? formStore.getBio().trim() : null);
        store.setAvatar(formStore.getAvatar());
        store.setCover(formStore.getCover());
        store.setFeaturedImages(formStore.getFeaturedImages());
        store.setIsOpen(formStore.getIsOpen());

        storeRepository.save(store);
        ra.addFlashAttribute("successMessage", "Cập nhật thông tin cửa hàng thành công!");
        return "redirect:/manager/stores";
    }
}
