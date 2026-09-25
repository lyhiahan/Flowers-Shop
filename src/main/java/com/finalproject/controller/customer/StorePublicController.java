package com.finalproject.controller.customer;

import com.finalproject.entity.Product;
import com.finalproject.entity.Store;
import com.finalproject.repository.ProductRepository;
import com.finalproject.repository.StoreRepository;
import com.finalproject.repository.UserFollowStoreRepository;
import com.finalproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/stores")
@RequiredArgsConstructor
public class StorePublicController {

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final UserFollowStoreRepository followStoreRepository;

    @GetMapping
    public String listStores(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        List<Store> stores;
        if (keyword != null && !keyword.trim().isEmpty()) {
            stores = storeRepository.searchActiveStores(keyword.trim());
        } else {
            stores = storeRepository.findByStatusTrue();
        }

        model.addAttribute("stores", stores);
        model.addAttribute("keyword", keyword != null ? keyword.trim() : "");
        return "customer/stores";
    }

    @GetMapping("/{id}")
    public String viewStore(@PathVariable("id") Long id, Principal principal, Model model) {
        Store store = storeRepository.findById(id).orElse(null);
        if (store == null) {
            return "redirect:/stores";
        }

        List<Product> products = productRepository.findByStoreIdOrderByCreatedAtDesc(id);

        boolean isFollowed = false;
        if (principal != null) {
            var userOpt = userRepository.findByUsername(principal.getName());
            if (userOpt.isPresent()) {
                isFollowed = followStoreRepository.findByUserIdAndStoreId(userOpt.get().getId(), id).isPresent();
            }
        }

        model.addAttribute("store", store);
        model.addAttribute("products", products);
        model.addAttribute("isFollowed", isFollowed);
        return "customer/store-detail";
    }
}
