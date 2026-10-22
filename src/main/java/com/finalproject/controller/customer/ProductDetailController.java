package com.finalproject.controller.customer;

import com.finalproject.entity.Product;
import com.finalproject.entity.User;
import com.finalproject.repository.ProductImageRepository;
import com.finalproject.repository.ProductRepository;
import com.finalproject.repository.UserRepository;
import com.finalproject.service.CartService;
import com.finalproject.service.ProductService;
import com.finalproject.service.ReviewService;
import com.finalproject.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ProductDetailController {

    private final ProductService productService;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ReviewService reviewService;
    private final CloudinaryService cloudinaryService;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final com.finalproject.repository.StoreInventoryRepository inventories;

    @GetMapping({"/product/{id}", "/products/{id}"})
    public String viewProductDetail(@PathVariable("id") Long id,
                                    @RequestParam(value = "reviewOrderId", required = false) Long reviewOrderId,
                                    Principal principal, Model model) {
        Product product = productService.getProductById(id);

        if (!com.finalproject.service.ProductAvailability.isPublic(product)) throw new com.finalproject.exception.ResourceNotFoundException("Sản phẩm không còn được bán.");

        // Tăng lượt xem
        productRepository.incrementViewCount(id);

        // Ghi nhận sản phẩm đã xem nếu đã đăng nhập
        boolean isLiked = false;
        boolean canReview = false;
        Long eligibleOrderId = null;
        if (principal != null) {
            userRepository.findByUsername(principal.getName()).ifPresent(u -> {
                productService.recordRecentlyViewed(u.getId(), id);
            });
            User u = userRepository.findByUsername(principal.getName()).orElse(null);
            if (u != null) {
                isLiked = productService.isWishlisted(u.getId(), id);
                eligibleOrderId = reviewOrderId == null ? reviewService.getEligibleOrderId(u.getId(), id)
                        : (reviewService.canReviewOrder(u.getId(), id, reviewOrderId) ? reviewOrderId : null);
                canReview = eligibleOrderId != null;
                try {
                    model.addAttribute("cartItemCount", cartService.getCartItemCount(principal.getName()));
                } catch (Exception ignored) {}
            }
        }

        model.addAttribute("product", product);
        var branchStock = inventories.findByProductId(product.getId()).stream()
                .filter(com.finalproject.entity.StoreInventory::isAvailableForSale)
                .filter(s -> Boolean.TRUE.equals(s.getStore().getStatus()) && Boolean.TRUE.equals(s.getStore().getIsOpen()))
                .filter(s -> product.getStore() == null || product.getStore().getId().equals(s.getStore().getId()))
                .sorted(java.util.Comparator.comparing(s -> s.getStore().getId())).toList();
        var selected = branchStock.stream().filter(s -> s.getQuantity() != null && s.getQuantity() > 0).findFirst().orElse(null);
        int availableStock = selected == null ? 0 : selected.getQuantity();
        model.addAttribute("purchaseStore", selected == null ? product.getStore() : selected.getStore());
        model.addAttribute("availableStock", availableStock);
        var images = productImageRepository.findByProductId(id);
        if (images.isEmpty() && product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            images = java.util.List.of(com.finalproject.entity.ProductImage.builder()
                    .imageUrl(product.getImageUrl())
                    .product(product)
                    .isMain(true)
                    .build());
        }
        model.addAttribute("images", images);
        model.addAttribute("reviews", reviewService.getReviewsByProduct(id));
        model.addAttribute("isLiked", isLiked);
        model.addAttribute("canReview", canReview);
        model.addAttribute("eligibleOrderId", eligibleOrderId);
        model.addAttribute("isFollowed", isLiked);
        model.addAttribute("followerCount", productService.getFollowerCount(id));
        model.addAttribute("relatedProducts", product.getCategory() != null 
                ? productRepository.findByCategoryId(product.getCategory().getId()).stream().filter(com.finalproject.service.ProductAvailability::isPublic).limit(4).toList()
                : java.util.List.of());

        return "customer/product-detail";
    }

    @PostMapping({"/product/{id}/review", "/products/{id}/review"})
    public String submitReview(@PathVariable("id") Long id,
                               @RequestParam(value = "rating", defaultValue = "0") int rating,
                               @RequestParam(value = "content", defaultValue = "") String content,
                               @RequestParam(value = "orderId", required = false) Long orderId,
                               @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                               Principal principal,
                               RedirectAttributes ra) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = userRepository.findByUsername(principal.getName()).orElseThrow();

        String error = null;
        boolean eligibleOrder = orderId != null && reviewService.canReviewOrder(user.getId(), id, orderId);
        if (!eligibleOrder) {
            error = "Bạn chỉ có thể đánh giá mẫu hoa trong đơn đã nhận và chưa đánh giá.";
        } else if (rating < 1 || rating > 5) {
            error = "Vui lòng chọn từ 1 đến 5 sao.";
        } else if (content.trim().length() < 50 || content.trim().length() > 1000) {
            error = "Nội dung đánh giá cần từ 50 đến 1.000 ký tự (hiện có " + content.trim().length() + ").";
        }
        if (error != null) {
            ra.addFlashAttribute("reviewError", error);
            ra.addFlashAttribute("reviewContent", content);
            ra.addFlashAttribute("reviewRating", rating);
            return "redirect:/product/" + id + "?reviewOrderId=" + (orderId == null ? "" : orderId)
                    + (eligibleOrder ? "#review-form" : "#review-section");
        }

        CloudinaryService.UploadResult uploaded = null;
        try {
            if (imageFile != null && !imageFile.isEmpty()) uploaded = cloudinaryService.upload(imageFile, "reviews");
            reviewService.addReview(user.getId(), id, orderId, rating, content,
                    uploaded == null ? null : uploaded.secureUrl());
            ra.addFlashAttribute("reviewSuccess", "Đánh giá của bạn đã được đăng. Cảm ơn bạn!");
            return "redirect:/product/" + id + "#review-list";
        } catch (Exception ex) {
            if (uploaded != null) cloudinaryService.delete(uploaded.publicId());
            ra.addFlashAttribute("reviewError", ex instanceof IllegalArgumentException || ex instanceof IllegalStateException
                    ? ex.getMessage() : "Chưa thể lưu đánh giá. Vui lòng thử lại.");
            ra.addFlashAttribute("reviewContent", content);
            ra.addFlashAttribute("reviewRating", rating);
            return "redirect:/product/" + id + "?reviewOrderId=" + orderId + "#review-form";
        }
    }
}
