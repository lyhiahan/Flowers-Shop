package com.finalproject.controller.customer;

import com.finalproject.entity.User;
import com.finalproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserPublicController {

    private final UserRepository userRepository;
    private final com.finalproject.service.CartService cartService;

    @GetMapping
    public String listUsers(@RequestParam(value = "keyword", required = false) String keyword,
                            @RequestParam(value = "page", defaultValue = "0") int page,
                            @RequestParam(value = "size", defaultValue = "6") int size,
                            java.security.Principal principal,
                            Model model) {
        String kw = keyword == null ? "" : keyword.trim();
        int pageSize = Math.min(50, Math.max(1, size));
        var sort = org.springframework.data.domain.Sort.by("id");
        java.util.function.IntFunction<org.springframework.data.domain.Page<User>> fetch = requested -> {
            var pageable = org.springframework.data.domain.PageRequest.of(requested, pageSize, sort);
            return kw.isEmpty() ? userRepository.findAll(pageable)
                    : userRepository.findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(kw, kw, kw, pageable);
        };
        long totalUsers = kw.isEmpty() ? userRepository.count()
                : userRepository.countByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(kw, kw, kw);
        int totalPages = (int)Math.max(1, Math.min(Integer.MAX_VALUE, (totalUsers + pageSize - 1) / pageSize));
        int currentPage = Math.max(0, Math.min(page, totalPages - 1));
        var result = fetch.apply(currentPage);
        List<User> pagedUsers = result.getContent();

        model.addAttribute("users", pagedUsers);
        model.addAttribute("keyword", keyword != null ? keyword.trim() : "");
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("hasPrevious", currentPage > 0);
        model.addAttribute("hasNext", currentPage < totalPages - 1);
        if (principal != null) {
            try {
                model.addAttribute("cartItemCount", cartService.getCartItemCount(principal.getName()));
            } catch (Exception ignored) {}
        }
        return "customer/users";
    }

    @GetMapping("/{id}")
    public String viewUser(@PathVariable("id") Long id,
                           @RequestParam(value = "tab", defaultValue = "home") String tab,
                           java.security.Principal principal,
                           Model model) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return "redirect:/users";
        }

        model.addAttribute("user", user);
        model.addAttribute("activeTab", tab);
        if (principal != null) {
            try {
                model.addAttribute("cartItemCount", cartService.getCartItemCount(principal.getName()));
            } catch (Exception ignored) {}
        }
        return "customer/user-detail";
    }
}

