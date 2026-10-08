package com.finalproject.controller.customer;

import com.finalproject.entity.User;
import com.finalproject.repository.UserRepository;
import com.finalproject.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class ProfileController {

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final com.finalproject.service.UserService userService;
    private final com.finalproject.service.ProductService productService;
    private final com.finalproject.repository.UserFollowStoreRepository followStoreRepository;
    private final com.finalproject.repository.StoreRepository storeRepository;
    private final com.finalproject.service.CartService cartService;
    private final com.finalproject.repository.OtpVerificationRepository otpRepository;
    private final com.finalproject.service.EmailService emailService;

    @GetMapping({"", "/profile"})
    public String viewProfile(@RequestParam(value = "tab", required = false) String activeTab,
                              Principal principal, Model model) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new AccessDeniedException("Người dùng không tồn tại"));
        model.addAttribute("user", user);
        model.addAttribute("addresses", userService.getUserAddresses(user.getId()));
        model.addAttribute("stores", storeRepository.findByStatusTrue());
        model.addAttribute("activeTab", java.util.Set.of("info", "address", "password").contains(activeTab == null ? "" : activeTab) ? activeTab : "info");
        try {
            model.addAttribute("cartItemCount", cartService.getCartItemCount(principal.getName()));
        } catch (Exception ignored) {}
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
            userService.updateProfileImages(principal.getName(), fullName, phone, address, avatarFile, coverFile);

            ra.addFlashAttribute("successMessage", "Cập nhật hồ sơ thành công!");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Lỗi cập nhật: " + ex.getMessage());
        }

        return "redirect:/customer/profile";
    }

    @PostMapping({"/send-password-otp", "/profile/send-password-otp"})
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> sendPasswordOtp(Principal principal, @RequestParam("email") String email) {
        if (principal == null) {
            return org.springframework.http.ResponseEntity.status(401).body(java.util.Map.of("success", false, "message", "Vui lòng đăng nhập!"));
        }
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            return org.springframework.http.ResponseEntity.status(404).body(java.util.Map.of("success", false, "message", "Không tìm thấy người dùng!"));
        }

        if (email == null || email.trim().isEmpty() || !email.trim().equalsIgnoreCase(user.getEmail())) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of(
                    "success", false,
                    "message", "Email không khớp với email tài khoản của bạn (" + user.getEmail() + ")!"
            ));
        }

        if (otpRepository.countByUserIdAndCreatedAtAfter(user.getId(), java.time.LocalDateTime.now().minusMinutes(10)) >= 5) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of(
                    "success", false,
                    "message", "Bạn đã yêu cầu gửi OTP quá nhiều lần. Vui lòng thử lại sau 10 phút!"
            ));
        }

        String code = com.finalproject.utils.OtpUtil.generateOtp();
        com.finalproject.entity.OtpVerification otp = com.finalproject.entity.OtpVerification.builder()
                .user(user)
                .code(code)
                .type(com.finalproject.enums.OtpType.RESET_PASSWORD)
                .expiredAt(java.time.LocalDateTime.now().plusMinutes(5))
                .createdAt(java.time.LocalDateTime.now())
                .isUsed(false)
                .build();
        otpRepository.save(otp);

        try {
            emailService.sendOtpEmail(user.getEmail(), code);
        } catch (Exception ex) {
            log.warn("Email send failed (SMTP): {}, fallback OTP: {}", ex.getMessage(), code);
        }
        log.info("[OTP ĐỔI MẬT KHẨU] User: {}, Email: {}, Mã OTP: {}", user.getUsername(), user.getEmail(), code);

        return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "message", "Mã xác thực OTP đã được gửi đến: " + user.getEmail()
        ));
    }

    @PostMapping({"/change-password", "/profile/change-password"})
    public String changePassword(Principal principal,
                                 @RequestParam("oldPassword") String oldPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam(value = "otpCode", required = false) String otpCode,
                                 RedirectAttributes ra) {
        if (principal == null) return "redirect:/login";
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();

        if (otpCode == null || otpCode.trim().length() != 6) {
            ra.addFlashAttribute("errorMessage", "Vui lòng nhập đầy đủ mã OTP 6 chữ số đã gửi về email!");
            return "redirect:/customer/profile?tab=password#tabPassword";
        }

        var otpOpt = otpRepository.findTopByUserIdAndCodeAndTypeAndIsUsedFalseOrderByExpiredAtDesc(
                user.getId(), otpCode.trim(), com.finalproject.enums.OtpType.RESET_PASSWORD);

        if (otpOpt.isEmpty() || otpOpt.get().getExpiredAt().isBefore(java.time.LocalDateTime.now())) {
            ra.addFlashAttribute("errorMessage", "Mã OTP không chính xác hoặc đã hết hạn (5 phút). Vui lòng nhấn gửi lại mã!");
            return "redirect:/customer/profile?tab=password#tabPassword";
        }

        try {
            userService.changePassword(principal.getName(), oldPassword, newPassword);
            var otp = otpOpt.get();
            otp.setIsUsed(true);
            otpRepository.save(otp);
            ra.addFlashAttribute("successMessage", "Đổi mật khẩu bảo mật thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/customer/profile?tab=password#tabPassword";
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
        return "redirect:/customer/profile?tab=address";
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
        return "redirect:/customer/profile?tab=address";
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
        return "redirect:/customer/profile?tab=address";
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
        return "redirect:/customer/profile?tab=address";
    }

    @GetMapping({"/wishlist", "/profile/wishlist"})
    public String viewWishlist(Principal principal, Model model) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        model.addAttribute("wishlist", productService.getWishlist(user.getId()));
        model.addAttribute("stores", storeRepository.findByStatusTrue());
        return "customer/wishlist";
    }

    // UC_015 Main Flow 4, 5, 6, 7: Chuyển đến trang Following & hiển thị bộ sưu tầm các sản phẩm đang theo dõi
    @GetMapping({"/following", "/profile/following"})
    public String viewFollowingPage(Principal principal,
                                    @RequestParam(value = "tab", defaultValue = "products") String tab,
                                    Model model) {
        if (principal == null) return "redirect:/login";
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        var followedProducts = productService.getWishlist(user.getId());
        var followedStores = followStoreRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(com.finalproject.entity.UserFollowStore::getStore).toList();
        model.addAttribute("followedProducts", followedProducts);
        model.addAttribute("followedStores", followedStores);
        model.addAttribute("activeTab", tab != null ? tab : "products");
        try {
            model.addAttribute("cartItemCount", cartService.getCartItemCount(principal.getName()));
        } catch (Exception ignored) {}
        return "customer/following";
    }

    // UC_015 Step 2 & 3: Chọn nút Follow/Following -> Theo dõi hoặc hủy theo dõi
    @PostMapping({"/wishlist/toggle/{productId}", "/profile/wishlist/toggle/{productId}", "/following/toggle/{productId}", "/following/product/{productId}", "/products/{productId}/follow"})
    public Object toggleWishlist(Principal principal, @PathVariable("productId") Long productId,
                                 @RequestHeader(value = "Referer", required = false) String referer,
                                 @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
                                 RedirectAttributes ra) {
        if (principal == null) {
            if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
                return org.springframework.http.ResponseEntity.status(401).body(java.util.Map.of("success", false, "message", "Vui lòng đăng nhập để lưu sản phẩm!"));
            }
            return "redirect:/login";
        }
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        boolean liked = productService.toggleWishlist(user.getId(), productId);
        String message = liked ? "Đã theo dõi sản phẩm thành công!" : "Đã hủy theo dõi sản phẩm!";

        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            int remainingCount = productService.getWishlist(user.getId()).size();
            return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
                    "success", true,
                    "liked", liked,
                    "remainingCount", remainingCount,
                    "message", message
            ));
        }

        ra.addFlashAttribute("successMessage", message);
        return referer != null ? "redirect:" + referer : "redirect:/customer/following";
    }

    @GetMapping({"/recently-viewed", "/profile/recently-viewed"})
    public String viewRecentlyViewed(Principal principal, Model model) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        model.addAttribute("recentProducts", productService.getRecentlyViewed(user.getId()));
        try {
            model.addAttribute("cartItemCount", cartService.getCartItemCount(principal.getName()));
        } catch (Exception ignored) {}
        return "customer/recently-viewed";
    }

    @GetMapping({"/followed-stores", "/profile/followed-stores"})
    public String viewFollowedStores(Principal principal, Model model) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        var follows = followStoreRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        model.addAttribute("followedStores", follows.stream().map(com.finalproject.entity.UserFollowStore::getStore).toList());
        try {
            model.addAttribute("cartItemCount", cartService.getCartItemCount(principal.getName()));
        } catch (Exception ignored) {}
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