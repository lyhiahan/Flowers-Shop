package com.finalproject.controller.admin;

import com.finalproject.entity.Transaction;
import com.finalproject.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestParam;

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
    public String listTransactions(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            Model model) {
        try {
            Pageable pageable = PageRequest.of(Math.max(0, page), size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
            Page<Transaction> transactionPage;
            if (keyword != null && !keyword.trim().isEmpty()) {
                transactionPage = transactionRepository.findByDescriptionContainingIgnoreCase(keyword.trim(), pageable);
            } else {
                transactionPage = transactionRepository.findAll(pageable);
            }
            model.addAttribute("transactionPage", transactionPage);
            model.addAttribute("transactions", transactionPage.getContent());
            model.addAttribute("currentPage", transactionPage.getNumber());
            model.addAttribute("totalPages", transactionPage.getTotalPages());
            model.addAttribute("totalElements", transactionPage.getTotalElements());
            model.addAttribute("pageSize", size);
            model.addAttribute("keyword", keyword != null ? keyword : "");
        } catch (Exception ex) {
            model.addAttribute("dataLoadError", true);
            model.addAttribute("errorMessage", "Không thể tải danh sách giao dịch: " + ex.getMessage());
            model.addAttribute("transactions", List.of());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalElements", 0L);
        }
        return "admin/transactions";
    }
}
