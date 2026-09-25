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

    @GetMapping
    public String listUsers(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        List<User> users;
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            users = userRepository.findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(kw, kw, kw);
        } else {
            users = userRepository.findAll();
        }

        model.addAttribute("users", users);
        model.addAttribute("keyword", keyword != null ? keyword.trim() : "");
        return "customer/users";
    }

    @GetMapping("/{id}")
    public String viewUser(@PathVariable("id") Long id, Model model) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return "redirect:/users";
        }

        model.addAttribute("user", user);
        return "customer/user-detail";
    }
}
