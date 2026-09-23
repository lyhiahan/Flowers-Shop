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

    @GetMapping
    public String listTransactions(Model model) {
        List<Transaction> transactions = transactionRepository.findAllByOrderByCreatedAtDesc();
        model.addAttribute("transactions", transactions);
        return "admin/transactions";
    }
}
