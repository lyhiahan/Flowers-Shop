package com.finalproject.controller.customer;

import com.finalproject.entity.Product;
import com.finalproject.entity.Store;
import com.finalproject.repository.ProductRepository;
import com.finalproject.repository.ReviewRepository;
import com.finalproject.repository.StoreRepository;
import com.finalproject.repository.UserFollowStoreRepository;
import com.finalproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/stores")
@RequiredArgsConstructor
public class StorePublicController {

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final UserFollowStoreRepository followStoreRepository;

    @GetMapping
    public String listStores(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "6") int size,
            Principal principal,
            Model model) {
        int pageNumber = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageNumber, size);
        Page<Store> storePage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            storePage = storeRepository.searchActiveStores(keyword.trim(), pageable);
        } else {
            storePage = storeRepository.findByStatusTrue(pageable);
        }

        Set<Long> followedStoreIds = new HashSet<>();
        Map<Long, Long> storeFollowerCounts = new HashMap<>();
        for (Store s : storePage.getContent()) {
            storeFollowerCounts.put(s.getId(), followStoreRepository.countByStoreId(s.getId()));
        }

        if (principal != null) {
            userRepository.findByUsername(principal.getName()).ifPresent(user -> {
                followedStoreIds.addAll(followStoreRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                        .stream().map(f -> f.getStore().getId()).toList());
            });
        }

        model.addAttribute("storePage", storePage);
        model.addAttribute("stores", storePage.getContent());
        model.addAttribute("followedStoreIds", followedStoreIds);
        model.addAttribute("storeFollowerCounts", storeFollowerCounts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", storePage.getTotalPages());
        model.addAttribute("totalItems", storePage.getTotalElements());
        model.addAttribute("keyword", keyword != null ? keyword.trim() : "");
        return "customer/stores";
    }

    @GetMapping("/{id}")
    public String viewStore(
            @PathVariable("id") Long id,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "tab", required = false, defaultValue = "collection") String tab,
            Principal principal,
            Model model) {
        Store store = storeRepository.findById(id).orElse(null);
        if (store == null) {
            return "redirect:/stores";
        }

        List<Product> products;
        if (keyword != null && !keyword.trim().isEmpty()) {
            products = productRepository.findByStoreIdAndNameContainingIgnoreCase(id, keyword.trim());
        } else {
            products = productRepository.findByStoreIdOrderByCreatedAtDesc(id);
        }

        products = products.stream().filter(com.finalproject.service.ProductAvailability::isVisible).toList();
        var reviews = reviewRepository.findByStoreIdOrderByCreatedAtDesc(id);

        boolean isFollowed = false;
        if (principal != null) {
            var userOpt = userRepository.findByUsername(principal.getName());
            if (userOpt.isPresent()) {
                isFollowed = followStoreRepository.findByUserIdAndStoreId(userOpt.get().getId(), id).isPresent();
            }
        }
        long followerCount = followStoreRepository.countByStoreId(id);

        model.addAttribute("store", store);
        model.addAttribute("products", products);
        model.addAttribute("reviews", reviews);
        model.addAttribute("isFollowed", isFollowed);
        model.addAttribute("followerCount", followerCount);
        model.addAttribute("keyword", keyword != null ? keyword.trim() : "");
        model.addAttribute("currentTab", tab);
        return "customer/store-detail";
    }

    // UC_015: Toggle Follow / Unfollow Store
    @PostMapping("/{id}/follow")
    public Object toggleFollowStorePublic(Principal principal, @PathVariable("id") Long storeId,
                                          @RequestHeader(value = "Referer", required = false) String referer,
                                          @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
                                          RedirectAttributes ra) {
        if (principal == null) {
            if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
                return org.springframework.http.ResponseEntity.status(401).body(java.util.Map.of("success", false, "message", "Vui lòng đăng nhập!"));
            }
            return "redirect:/login";
        }
        var user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";
        var existing = followStoreRepository.findByUserIdAndStoreId(user.getId(), storeId);
        boolean followed;
        String message;
        if (existing.isPresent()) {
            followStoreRepository.delete(existing.get());
            followed = false;
            message = "Đã hủy theo dõi cửa hàng thành công!";
        } else {
            var store = storeRepository.findById(storeId).orElse(null);
            if (store != null) {
                followStoreRepository.save(com.finalproject.entity.UserFollowStore.builder().user(user).store(store).build());
            }
            followed = true;
            message = "Đã theo dõi cửa hàng thành công!";
        }
        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
                    "success", true,
                    "followed", followed,
                    "message", message
            ));
        }
        ra.addFlashAttribute("successMessage", message);
        return referer != null ? "redirect:" + referer : "redirect:/stores/" + storeId;
    }
}
