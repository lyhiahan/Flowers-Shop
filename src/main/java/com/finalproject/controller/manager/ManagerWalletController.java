package com.finalproject.controller.manager;

import com.finalproject.entity.Store;
import com.finalproject.entity.Transaction;
import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.repository.StoreRepository;
import com.finalproject.repository.TransactionRepository;
import com.finalproject.repository.UserRepository;
import com.finalproject.service.StoreAccessService;
import com.finalproject.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Collections;
import java.util.List;

/**
 * Controller triển khai Use Case UC_023: Quản lý ví điện tử của cửa hàng (Store Management – Ewallet)
 */
@Controller
@RequestMapping({"/manager/wallet", "/vendor/wallet", "/manager/gd-coins", "/vendor/gd-coins"})
@RequiredArgsConstructor
public class ManagerWalletController {

    private final UserRepository userRepository;
    private final WalletService walletService;
    private final StoreAccessService storeAccess;
    private final StoreRepository storeRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Bước 6 & 7: Vào trang quản lý ví điện tử cửa hàng (GD Coins)
     * Bước 8: Phân trang danh sách giao dịch (Next, Previous, hoặc số trang bất kỳ)
     * Bước 9: Nút Withdraw (hiển thị khi người quản lý là chủ cửa hàng)
     */
    @GetMapping
    public String viewWallet(@RequestParam(value = "page", defaultValue = "0") int page,
                             @RequestParam(value = "size", defaultValue = "8") int size,
                             Principal principal, Model model) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Store store = resolveStore(user);
        if (store == null) {
            return "redirect:/manager/stores";
        }
        storeAccess.requireStore(store);

        boolean isOwner = (store.getOwner() != null && user.getId().equals(store.getOwner().getId()))
                || user.getRole() == Role.ADMIN;

        List<Transaction> allTransactions = transactionRepository.findByStoreIdOrderByCreatedAtDesc(store.getId());

        // Bước 8: Phân trang giao dịch
        int totalElements = allTransactions.size();
        int totalPages = totalElements > 0 ? (int) Math.ceil((double) totalElements / size) : 1;
        if (page < 0) page = 0;
        if (page >= totalPages && totalElements > 0) page = totalPages - 1;

        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<Transaction> pagedTransactions = totalElements > 0 ? allTransactions.subList(fromIndex, toIndex) : Collections.emptyList();

        model.addAttribute("store", store);
        model.addAttribute("currentUser", user);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("transactions", pagedTransactions);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalElements", totalElements);

        return "manager/wallet";
    }

    /**
     * Bước 10: Hiển thị form rút tiền
     * Bước 11: Nhập số tiền và xác nhận mật khẩu
     * Bước 12: Hiển thị thông báo xác nhận
     * Bước 13: Bấm nút Confirm
     * Bước 14: Thông báo thành công
     * 14a: Bấm nút Cancel -> 15a: Hủy thao tác
     */
    @PostMapping("/withdraw")
    public String requestWithdrawal(Principal principal,
                                    @RequestParam("amount") BigDecimal amount,
                                    @RequestParam(value = "password", required = false) String password,
                                    @RequestParam(value = "bankName", required = false) String bankName,
                                    @RequestParam(value = "accountNumber", required = false) String accountNumber,
                                    @RequestParam(value = "accountHolder", required = false) String accountHolder,
                                    RedirectAttributes ra) {
        try {
            User user = userRepository.findByUsername(principal.getName()).orElseThrow();
            Store store = resolveStore(user);
            if (store == null) {
                return "redirect:/manager/stores";
            }
            storeAccess.requireStore(store);

            // Bước 9: Nút này sẽ tồn tại nếu người quản lý là chủ cửa hàng
            boolean isOwner = (store.getOwner() != null && user.getId().equals(store.getOwner().getId()))
                    || user.getRole() == Role.ADMIN;
            if (!isOwner) {
                throw new AccessDeniedException("Chỉ chủ cửa hàng mới có quyền thực hiện rút tiền từ ví cửa hàng.");
            }

            // Bước 11: Xác nhận mật khẩu
            if (password == null || password.trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập mật khẩu xác nhận.");
            }
            boolean passwordValid = (user.getPassword() != null && user.getPassword().equals(password))
                    || passwordEncoder.matches(password, user.getPassword());
            if (!passwordValid) {
                throw new IllegalArgumentException("Mật khẩu xác nhận không chính xác. Vui lòng thử lại!");
            }

            if (bankName == null || bankName.isBlank()) bankName = "Vietcombank";
            if (accountNumber == null || accountNumber.isBlank()) accountNumber = "1012345678";
            if (accountHolder == null || accountHolder.isBlank()) {
                accountHolder = user.getFullName() != null ? user.getFullName() : user.getUsername();
            }

            walletService.requestWithdrawal(store, amount, bankName, accountNumber, accountHolder);
            ra.addFlashAttribute("successMessage", "Yêu cầu rút " + String.format("%,d", amount.longValue()) + " VNĐ thành công! Hệ thống đang xử lý.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/manager/wallet";
    }

    private Store resolveStore(User user) {
        if (user.getStore() != null) {
            return user.getStore();
        }
        List<Store> owned = storeRepository.findByOwnerId(user.getId());
        if (!owned.isEmpty()) {
            Store first = owned.get(0);
            user.setStore(first);
            userRepository.save(user);
            return first;
        }
        return null;
    }
}