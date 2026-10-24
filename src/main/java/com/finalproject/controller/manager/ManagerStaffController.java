package com.finalproject.controller.manager;

import com.finalproject.entity.Store;
import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.repository.UserRepository;
import com.finalproject.service.StoreAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

/**
 * Controller triển khai Use Case UC_020: Quản lý nhân viên của cửa hàng (Store Management – Staff)
 */
@Controller
@RequestMapping({"/manager/staff", "/vendor/staff", "/manager/staffs", "/vendor/staffs"})
@RequiredArgsConstructor
@Slf4j
public class ManagerStaffController {

    private final UserRepository userRepository;
    private final StoreAccessService storeAccess;
    private final com.finalproject.service.StoreStaffManagementService staffManagement;

    /**
     * Bước 6 & 7: Vào trang quản lý nhân viên cửa hàng
     * Bước 8, 9: Xem thông tin chủ cửa hàng (Shop owner)
     * Bước 10, 11: Xem danh sách nhân viên cửa hàng (Shop staffs)
     * Bước 12: Phân trang danh sách nhân viên (Next, Previous, số trang)
     */
    @GetMapping
    public String listStaff(@RequestParam(value = "tab", defaultValue = "staffs") String tab,
                            @RequestParam(value = "page", defaultValue = "0") int page,
                            @RequestParam(value = "size", defaultValue = "6") int size,
                            @RequestParam(value="candidateKeyword", defaultValue="") String candidateKeyword,
                            @RequestParam(value="candidatePage", defaultValue="0") int candidatePage,
                            Principal principal, Model model) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Store store = resolveStore(user);
        if (store == null) {
            return "redirect:/manager/stores";
        }
        storeAccess.requireManager(store);

        // Kiểm tra xem người quản lý có phải là Chủ cửa hàng hay Đối tác (Partner)
        boolean isOwner = user.hasRole(Role.ADMIN) || (store.getOwner() != null && user.getId().equals(store.getOwner().getId()));
        boolean isPartner = !isOwner;

        // Chủ cửa hàng (Shop owner)
        User shopOwner = store.getOwner();

        var result = com.finalproject.utils.StorePages.load(page, size, pageable -> userRepository.findStoreStaff(store.getId(), shopOwner == null ? -1L : shopOwner.getId(), pageable));
        List<User> pagedStaff = result.getContent();
        com.finalproject.utils.StorePages.model(model, result);
        var candidates = com.finalproject.utils.StorePages.load(candidatePage, 20, pageable -> userRepository.findAll((root, query, cb) -> {
            var eligibility = cb.or(cb.equal(root.get("role"), Role.STAFF), cb.isMember(Role.STAFF, root.get("roles")));
            var owned = query.subquery(Long.class);
            var ownedStore = owned.from(Store.class);
            owned.select(ownedStore.get("id")).where(cb.equal(ownedStore.get("owner"), root));
            var linked = query.subquery(Long.class);
            var membership = linked.from(com.finalproject.entity.StoreMembership.class);
            linked.select(membership.get("id")).where(cb.equal(membership.get("user"), root), cb.isTrue(membership.get("active")));
            String kw = "%" + candidateKeyword.trim().toLowerCase() + "%";
            return cb.and(eligibility, cb.isNull(root.get("store")), cb.not(cb.exists(owned)), cb.not(cb.exists(linked)),
                    cb.notEqual(root.get("role"), Role.ADMIN), cb.isNotMember(Role.ADMIN, root.get("roles")),
                    cb.notEqual(root.get("role"), Role.MANAGER), cb.isNotMember(Role.MANAGER, root.get("roles")),
                    cb.isTrue(root.get("isActive")), cb.isFalse(root.get("accountDisabledByAdmin")), cb.notEqual(root.get("id"), user.getId()),
                    cb.or(cb.like(cb.lower(root.get("username")), kw), cb.like(cb.lower(root.get("fullName")), kw)));
        }, pageable));
        List<User> candidateUsers = candidates.getContent();
        model.addAttribute("candidateKeyword", candidateKeyword);
        model.addAttribute("candidatePage", candidates.getNumber());
        model.addAttribute("candidatePages", Math.max(1, candidates.getTotalPages()));
        model.addAttribute("store", store);
        model.addAttribute("shopOwner", shopOwner);
        model.addAttribute("staffList", pagedStaff);
        model.addAttribute("totalStaffCount", result.getTotalElements());
        model.addAttribute("activeTab", tab);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("isPartner", isPartner);
        model.addAttribute("candidateUsers", candidateUsers);
        model.addAttribute("currentUser", user);

        return "manager/staff";
    }

    /**
     * Bước 13-18: Thêm nhân viên vào cửa hàng (Chỉ chủ cửa hàng có quyền)
     * Bước 15: Chọn các nhân viên muốn thêm (hỗ trợ chọn nhiều checkbox hoặc nhập username mới)
     * Bước 16: Bấm nút Submit -> Bước 17: Bấm nút Confirm -> Bước 18: Thông báo thành công
     */
    @PostMapping("/add")
    public String addStaff(Principal principal,
                           @RequestParam(value = "selectedUserIds", required = false) List<Long> selectedUserIds,
                           @RequestParam(value = "username", required = false) String username,
                           @RequestParam(value = "fullName", required = false) String fullName,
                           @RequestParam(value = "email", required = false) String email,
                           @RequestParam(value = "phone", required = false) String phone,
                           @RequestParam(value = "password", required = false) String password,
                           @RequestParam(value = "role", defaultValue = "STAFF") String roleStr,
                           RedirectAttributes ra) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Store store = resolveStore(user);
        if (store == null) return "redirect:/manager/stores";
        storeAccess.requireManager(store);

        boolean isOwner = user.hasRole(Role.ADMIN) || (store.getOwner() != null && user.getId().equals(store.getOwner().getId()));
        if (!isOwner) {
            throw new AccessDeniedException("Chỉ quản lý phụ trách chi nhánh hoặc admin có quyền thêm nhân viên.");
        }

        try {
            int addedCount = staffManagement.add(store, selectedUserIds, username, fullName, email, phone, password, roleStr);
            ra.addFlashAttribute("successMessage", "Đã thêm " + addedCount + " nhân viên vào cửa hàng thành công!");
        } catch (Exception ex) {
            log.error("Lỗi thêm nhân viên", ex);
            String msg = ex.getMessage();
            ra.addFlashAttribute("errorMessage", "Thêm nhân viên thất bại: " + msg);
            ra.addFlashAttribute("reopenAddStaffModal", true);
            ra.addFlashAttribute("prevUsername", username);
            ra.addFlashAttribute("prevFullName", fullName);
            ra.addFlashAttribute("prevEmail", email);
            ra.addFlashAttribute("prevPhone", phone);
            ra.addFlashAttribute("prevSelectedUserIds", selectedUserIds);

            String lower = msg != null ? msg.toLowerCase() : "";
            if (lower.contains("email")) {
                ra.addFlashAttribute("fieldError_email", msg);
                ra.addFlashAttribute("focusField", "inputStaffEmail");
            } else if (lower.contains("tên đăng nhập") || lower.contains("username")) {
                ra.addFlashAttribute("fieldError_username", msg);
                ra.addFlashAttribute("focusField", "inputStaffUsername");
            } else if (lower.contains("mật khẩu") || lower.contains("password")) {
                ra.addFlashAttribute("fieldError_password", msg);
                ra.addFlashAttribute("focusField", "inputStaffPassword");
            } else if (lower.contains("họ và tên") || lower.contains("họ tên")) {
                ra.addFlashAttribute("fieldError_fullName", msg);
                ra.addFlashAttribute("focusField", "inputStaffFullName");
            } else if (lower.contains("ứng viên") || lower.contains("chọn") || lower.contains("danh sách")) {
                ra.addFlashAttribute("fieldError_candidate", msg);
                ra.addFlashAttribute("focusField", "candidatesListContainer");
            } else if (lower.contains("số điện thoại") || lower.contains("phone")) {
                ra.addFlashAttribute("fieldError_phone", msg);
                ra.addFlashAttribute("focusField", "inputStaffPhone");
            }
        }

        return "redirect:/manager/staff?tab=staffs";
    }

    /**
     * Alternative Flow 13-15a: Nút Delete nhân viên (Chỉ chủ cửa hàng có quyền)
     */
    @PostMapping({"/remove/{id}", "/delete/{id}"})
    public String deleteStaff(@PathVariable("id") Long id,
                              Principal principal,
                              RedirectAttributes ra) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Store store = resolveStore(user);
        if (store == null) return "redirect:/manager/stores";
        storeAccess.requireManager(store);

        boolean isOwner = user.hasRole(Role.ADMIN) || (store.getOwner() != null && user.getId().equals(store.getOwner().getId()));
        if (!isOwner) {
            throw new AccessDeniedException("Chỉ quản lý phụ trách chi nhánh hoặc admin có quyền xóa nhân viên khỏi cửa hàng.");
        }

        staffManagement.remove(store, id);
        ra.addFlashAttribute("successMessage", "Đã xóa nhân viên khỏi cửa hàng thành công!");

        return "redirect:/manager/staff?tab=staffs";
    }

    /**
     * Alternative Flow 13-15b: Nút Out this shop (Dành cho đối tác / partner rời khỏi cửa hàng)
     */
    @PostMapping("/out-shop")
    public String outThisShop(Principal principal, RedirectAttributes ra) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Store store = resolveStore(user);
        if (store == null) return "redirect:/manager/stores";

        boolean isOwner = store.getOwner() != null && user.getId().equals(store.getOwner().getId());
        if (isOwner) {
            ra.addFlashAttribute("errorMessage", "Chủ cửa hàng không thể tự rời cửa hàng. Bạn cần chuyển nhượng hoặc xóa chi nhánh.");
            return "redirect:/manager/staff";
        }

        staffManagement.leave(store);

        ra.addFlashAttribute("successMessage", "Bạn đã rời khỏi cửa hàng [" + store.getName() + "] thành công!");
        return "redirect:/manager/stores";
    }

    private Store resolveStore(User user) {
        return storeAccess.selectedStore();
    }
}
