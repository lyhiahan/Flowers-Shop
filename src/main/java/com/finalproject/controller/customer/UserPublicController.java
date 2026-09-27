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
    public String listUsers(@RequestParam(value = "keyword", required = false) String keyword,
                            @RequestParam(value = "page", defaultValue = "0") int page,
                            @RequestParam(value = "size", defaultValue = "3") int size,
                            Model model) {
        List<User> allUsers;
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            allUsers = userRepository.findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(kw, kw, kw);
        } else {
            allUsers = userRepository.findAll();
        }

        int totalUsers = allUsers.size();
        int pageSize = Math.max(1, size);
        int totalPages = (int) Math.ceil((double) totalUsers / pageSize);
        if (totalPages == 0) totalPages = 1;

        int currentPage = Math.max(0, Math.min(page, totalPages - 1));
        int fromIndex = Math.min(currentPage * pageSize, totalUsers);
        int toIndex = Math.min(fromIndex + pageSize, totalUsers);
        List<User> pagedUsers = allUsers.subList(fromIndex, toIndex);

        model.addAttribute("users", pagedUsers);
        model.addAttribute("keyword", keyword != null ? keyword.trim() : "");
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("hasPrevious", currentPage > 0);
        model.addAttribute("hasNext", currentPage < totalPages - 1);
        return "customer/users";
    }

    @GetMapping("/{id}")
    public String viewUser(@PathVariable("id") Long id,
                           @RequestParam(value = "tab", defaultValue = "home") String tab,
                           Model model) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return "redirect:/users";
        }

        model.addAttribute("user", user);
        model.addAttribute("activeTab", tab);
        return "customer/user-detail";
    }
}

