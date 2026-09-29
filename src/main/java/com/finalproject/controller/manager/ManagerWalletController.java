package com.finalproject.controller.manager;

import com.finalproject.entity.Store;
import com.finalproject.entity.Transaction;
import com.finalproject.entity.User;
import com.finalproject.repository.StoreRepository;
import com.finalproject.repository.TransactionRepository;
import com.finalproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping({"/manager/wallet", "/vendor/wallet"})
@RequiredArgsConstructor
public class ManagerWalletController {

    private final UserRepository userRepository;
    private final com.finalproject.service.WalletService walletService;
    private final com.finalproject.service.StoreAccessService storeAccess;
    private final StoreRepository storeRepository;
    private final TransactionRepository transactionRepository;

    @GetMapping
    public String viewWallet(Principal principal, Model model) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Store store = storeAccess.assignedStore();

        List<Transaction> transactions = store != null ?
                transactionRepository.findByStoreIdOrderByCreatedAtDesc(store.getId()) :
                List.of();

        model.addAttribute("store", store);
        model.addAttribute("transactions", transactions);
        return "manager/wallet";
    }

    @PostMapping("/withdraw")
    public String requestWithdrawal(Principal principal,
                                    @RequestParam("amount") BigDecimal amount,
                                    @RequestParam("bankName") String bankName,
                                    @RequestParam("accountNumber") String accountNumber,
                                    @RequestParam("accountHolder") String accountHolder,
                                    RedirectAttributes ra) {
        try {
            walletService.requestWithdrawal(amount, bankName, accountNumber, accountHolder);
            ra.addFlashAttribute("successMessage", "Đã giữ số tiền yêu cầu và gửi lệnh rút cho quản trị viên xử lý.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/manager/wallet";
    }
}