package com.finalproject.controller.admin;

import com.finalproject.entity.Transaction;
import com.finalproject.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin/transactions")
@RequiredArgsConstructor
public class AdminTransactionController {

    private final TransactionRepository transactionRepository;
    private final com.finalproject.service.WalletService walletService;

    @org.springframework.web.bind.annotation.PostMapping("/{id}/resolve")
    public String resolve(@org.springframework.web.bind.annotation.PathVariable("id") Long id,
                          @org.springframework.web.bind.annotation.RequestParam("paid") boolean paid,
                          org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            walletService.resolveWithdrawal(id, paid);
            ra.addFlashAttribute("successMessage", paid ? "Đã ghi nhận chuyển tiền." : "Đã từ chối và trả lại số dư ví.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/transactions";
    }

    @GetMapping
    public String listTransactions(Model model) {
        List<Transaction> transactions = transactionRepository.findAllByOrderByCreatedAtDesc();
        model.addAttribute("transactions", transactions);
        return "admin/transactions";
    }
}
