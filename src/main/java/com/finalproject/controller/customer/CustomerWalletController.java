package com.finalproject.controller.customer;

import com.finalproject.service.CustomerWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@Controller @RequiredArgsConstructor
@RequestMapping("/customer/wallet")
public class CustomerWalletController {
    private final CustomerWalletService wallets;

    @GetMapping
    public String wallet(Principal principal, @RequestParam(defaultValue = "0") int page, Model model) {
        var history = wallets.history(principal.getName(), page);
        model.addAttribute("walletBalance", wallets.balance(principal.getName()));
        model.addAttribute("history", history);
        model.addAttribute("walletEnabled", wallets.isEnabled());
        return "customer/wallet";
    }
}
