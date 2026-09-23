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
    private final StoreRepository storeRepository;
    private final TransactionRepository transactionRepository;

    @GetMapping
    public String viewWallet(Principal principal, Model model) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Store store = user.getStore() != null ? user.getStore() : storeRepository.findById(1L).orElse(null);

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
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Store store = user.getStore() != null ? user.getStore() : storeRepository.findById(1L).orElseThrow();

        if (amount == null || amount.compareTo(BigDecimal.valueOf(50000)) < 0) {
            ra.addFlashAttribute("errorMessage", "Số tiền rút tối thiểu là 50.000 VNĐ!");
            return "redirect:/manager/wallet";
        }

        if (store.getEWallet() == null || store.getEWallet().compareTo(amount) < 0) {
            ra.addFlashAttribute("errorMessage", "Số dư ví không đủ để thực hiện lệnh rút!");
            return "redirect:/manager/wallet";
        }

        // Trừ tiền trong ví
        store.setEWallet(store.getEWallet().subtract(amount));
        storeRepository.save(store);

        // Ghi nhật ký giao dịch
        Transaction tx = Transaction.builder()
                .store(store)
                .amount(amount)
                .isUp(false)
                .type("WITHDRAWAL")
                .description("Rút tiền về ngân hàng " + bankName + " - STK: " + accountNumber + " (" + accountHolder + ")")
                .status("COMPLETED")
                .build();
        transactionRepository.save(tx);

        ra.addFlashAttribute("successMessage", "Lệnh rút tiền " + amount + "đ thành công!");
        return "redirect:/manager/wallet";
    }
}
