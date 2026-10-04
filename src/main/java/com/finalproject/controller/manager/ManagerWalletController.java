package com.finalproject.controller.manager;

import com.finalproject.entity.Store;
import com.finalproject.entity.Transaction;
import com.finalproject.entity.User;
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
        storeAccess.requireManager(store);

        boolean isOwner = (store.getOwner() != null && user.getId().equals(store.getOwner().getId()));

        var result = com.finalproject.utils.StorePages.load(page, size, pageable -> transactionRepository.findByStoreId(store.getId(), pageable));
        List<Transaction> pagedTransactions = result.getContent();
        com.finalproject.utils.StorePages.model(model, result);
        model.addAttribute("store", store);
        model.addAttribute("currentUser", user);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("requestId", java.util.UUID.randomUUID().toString());
        model.addAttribute("transactions", pagedTransactions);

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
                                    @RequestParam(value = "requestId", required = false) String requestId,
                                    RedirectAttributes ra) {
        try {
            User user = userRepository.findByUsername(principal.getName()).orElseThrow();
            Store store = resolveStore(user);
            if (store == null) {
                return "redirect:/manager/stores";
            }
            storeAccess.requireManager(store);

            // Bước 9: Nút này sẽ tồn tại nếu người quản lý là chủ cửa hàng
            boolean isOwner = (store.getOwner() != null && user.getId().equals(store.getOwner().getId()));
            if (!isOwner) {
                throw new AccessDeniedException("Chỉ chủ cửa hàng mới có quyền thực hiện rút tiền từ ví cửa hàng.");
            }

            // Bước 11: Xác nhận mật khẩu
            if (password == null || password.trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập mật khẩu xác nhận.");
            }
            boolean passwordValid = user.getPassword() != null && passwordEncoder.matches(password, user.getPassword());
            if (!passwordValid) {
                throw new IllegalArgumentException("Mật khẩu xác nhận không chính xác. Vui lòng thử lại!");
            }

            walletService.requestWithdrawal(store, amount, bankName, accountNumber, accountHolder, requestId);
            ra.addFlashAttribute("successMessage", "Yêu cầu rút " + String.format("%,d", amount.longValue()) + " VNĐ thành công! Hệ thống đang xử lý.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/manager/wallet";
    }

    private Store resolveStore(User user) {
        return storeAccess.selectedStore();
    }
}
