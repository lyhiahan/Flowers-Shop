package com.finalproject.controller.customer;

import com.finalproject.entity.User;
import com.finalproject.repository.UserRepository;
import com.finalproject.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping({"/customer/profile", "/customer"})
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final com.finalproject.service.UserService userService;
    private final com.finalproject.service.ProductService productService;
    private final com.finalproject.repository.UserFollowStoreRepository followStoreRepository;
    private final com.finalproject.repository.StoreRepository storeRepository;

    @GetMapping({"", "/profile"})
    public String viewProfile(Principal principal, Model model) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new AccessDeniedException("Người dùng không tồn tại"));
        model.addAttribute("user", user);
        model.addAttribute("addresses", userService.getUserAddresses(user.getId()));
        return "customer/profile";
    }

    @PostMapping({"/update", "/profile/update"})
    public String updateProfile(Principal principal,
                                @RequestParam("fullName") String fullName,
                                @RequestParam(value = "phone", required = false) String phone,
                                @RequestParam(value = "address", required = false) String address,
                                @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                                @RequestParam(value = "coverFile", required = false) MultipartFile coverFile,
                                RedirectAttributes ra) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();

        try {
            String avatarUrl = user.getAvatarUrl();
            if (avatarFile != null && !avatarFile.isEmpty()) {
                avatarUrl = cloudinaryService.uploadImage(avatarFile);
            }
            String coverUrl = user.getCover();
            if (coverFile != null && !coverFile.isEmpty()) {
                coverUrl = cloudinaryService.uploadImage(coverFile);
            }
            userService.updateProfile(principal.getName(), fullName, phone, address, avatarUrl, coverUrl);
            ra.addFlashAttribute("successMessage", "Cập nhật hồ sơ thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Lỗi cập nhật: " + ex.getMessage());
        }

        return "redirect:/customer/profile";
    }

    @PostMapping({"/change-password", "/profile/change-password"})
    public String changePassword(Principal principal,
                                 @RequestParam("oldPassword") String oldPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 RedirectAttributes ra) {
        try {
            userService.changePassword(principal.getName(), oldPassword, newPassword);
            ra.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/customer/profile";
    }

    @PostMapping({"/address/add", "/profile/address/add"})
    public String addAddress(Principal principal,
                             @RequestParam("recipientName") String recipientName,
                             @RequestParam("phone") String phone,
                             @RequestParam("addressDetail") String addressDetail,
                             @RequestParam(value = "isDefault", defaultValue = "false") boolean isDefault,
                             RedirectAttributes ra) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        try {
            userService.addAddress(user.getId(), recipientName, phone, addressDetail, isDefault);
            ra.addFlashAttribute("successMessage", "Thêm địa chỉ mới thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/customer/profile";
    }

    @PostMapping({"/address/update/{id}", "/profile/address/update/{id}"})
    public String updateAddress(Principal principal,
                                @PathVariable("id") Long id,
                                @RequestParam("recipientName") String recipientName,
                                @RequestParam("phone") String phone,
                                @RequestParam("addressDetail") String addressDetail,
                                @RequestParam(value = "isDefault", defaultValue = "false") boolean isDefault,
                                RedirectAttributes ra) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        try {
            userService.updateAddress(id, user.getId(), recipientName, phone, addressDetail, isDefault);
            ra.addFlashAttribute("successMessage", "Cập nhật địa chỉ nhận hàng thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/customer/profile";
    }

    @PostMapping({"/address/delete/{id}", "/profile/address/delete/{id}"})
    public String deleteAddress(Principal principal, @PathVariable("id") Long id, RedirectAttributes ra) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        try {
            userService.deleteAddress(id, user.getId());
            ra.addFlashAttribute("successMessage", "Xóa địa chỉ thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/customer/profile";
    }

    @PostMapping({"/address/set-default/{id}", "/profile/address/set-default/{id}"})
    public String setDefaultAddress(Principal principal, @PathVariable("id") Long id, RedirectAttributes ra) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        try {
            userService.setDefaultAddress(id, user.getId());
            ra.addFlashAttribute("successMessage", "Đã đặt làm địa chỉ mặc định!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/customer/profile";
    }

    @GetMapping({"/wishlist", "/profile/wishlist"})
    public String viewWishlist(Principal principal, Model model) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        model.addAttribute("wishlist", productService.getWishlist(user.getId()));
        return "customer/wishlist";
    }

    @PostMapping({"/wishlist/toggle/{productId}", "/profile/wishlist/toggle/{productId}"})
    public String toggleWishlist(Principal principal, @PathVariable("productId") Long productId,
                                 @RequestHeader(value = "Referer", required = false) String referer,
                                 RedirectAttributes ra) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        boolean liked = productService.toggleWishlist(user.getId(), productId);
        ra.addFlashAttribute("successMessage", liked ? "Đã thêm vào danh sách yêu thích ❤️" : "Đã bỏ khỏi danh sách yêu thích");
        return referer != null ? "redirect:" + referer : "redirect:/customer/wishlist";
    }

    @GetMapping({"/recently-viewed", "/profile/recently-viewed"})
    public String viewRecentlyViewed(Principal principal, Model model) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        model.addAttribute("recentProducts", productService.getRecentlyViewed(user.getId()));
        return "customer/recently-viewed";
    }

    @GetMapping({"/followed-stores", "/profile/followed-stores"})
    public String viewFollowedStores(Principal principal, Model model) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        var follows = followStoreRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        model.addAttribute("followedStores", follows.stream().map(com.finalproject.entity.UserFollowStore::getStore).toList());
        return "customer/followed-stores";
    }

    @PostMapping({"/store/follow/{storeId}", "/profile/store/follow/{storeId}"})
    public String toggleFollowStore(Principal principal, @PathVariable("storeId") Long storeId,
                                    @RequestHeader(value = "Referer", required = false) String referer,
                                    RedirectAttributes ra) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        var existing = followStoreRepository.findByUserIdAndStoreId(user.getId(), storeId);
        if (existing.isPresent()) {
            followStoreRepository.delete(existing.get());
            ra.addFlashAttribute("successMessage", "Đã hủy theo dõi cửa hàng");
        } else {
            var store = storeRepository.findById(storeId).orElseThrow();
            followStoreRepository.save(com.finalproject.entity.UserFollowStore.builder().user(user).store(store).build());
            ra.addFlashAttribute("successMessage", "Đã theo dõi cửa hàng thành công!");
        }
        return referer != null ? "redirect:" + referer : "redirect:/customer/followed-stores";
    }
}