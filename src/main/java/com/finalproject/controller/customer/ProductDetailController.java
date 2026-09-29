package com.finalproject.controller.customer;

import com.finalproject.entity.Product;
import com.finalproject.entity.User;
import com.finalproject.repository.ProductImageRepository;
import com.finalproject.repository.ProductRepository;
import com.finalproject.repository.UserRepository;
import com.finalproject.service.CartService;
import com.finalproject.service.ProductService;
import com.finalproject.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ProductDetailController {

    private final ProductService productService;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ReviewService reviewService;
    private final UserRepository userRepository;
    private final CartService cartService;

    @GetMapping({"/product/{id}", "/products/{id}"})
    public String viewProductDetail(@PathVariable("id") Long id, Principal principal, Model model) {
        Product product = productService.getProductById(id);

        // Tăng lượt xem
        product.setViewCount(product.getViewCount() != null ? product.getViewCount() + 1 : 1);
        productRepository.save(product);

        // Ghi nhận sản phẩm đã xem nếu đã đăng nhập
        boolean isLiked = false;
        if (principal != null) {
            userRepository.findByUsername(principal.getName()).ifPresent(u -> {
                productService.recordRecentlyViewed(u.getId(), id);
            });
            User u = userRepository.findByUsername(principal.getName()).orElse(null);
            if (u != null) {
                isLiked = productService.isWishlisted(u.getId(), id);
                try {
                    model.addAttribute("cartItemCount", cartService.getCartItemCount(principal.getName()));
                } catch (Exception ignored) {}
            }
        }

        model.addAttribute("product", product);
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
        model.addAttribute("isFollowed", isLiked);
        model.addAttribute("followerCount", productService.getFollowerCount(id));
        model.addAttribute("relatedProducts", productRepository.findByCategoryId(product.getCategory().getId()).stream().limit(4).toList());

        return "customer/product-detail";
    }

    @PostMapping({"/product/{id}/review", "/products/{id}/review"})
    public String submitReview(@PathVariable("id") Long id,
                               @RequestParam("rating") int rating,
                               @RequestParam("content") String content,
                               @RequestParam(value = "orderId", required = false) Long orderId,
                               @RequestParam(value = "imageUrl", required = false) String imageUrl,
                               Principal principal,
                               RedirectAttributes ra) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = userRepository.findByUsername(principal.getName()).orElseThrow();

        // Kiểm tra độ dài bình luận tối thiểu 50 ký tự theo yêu cầu đề cương
        if (content == null || content.trim().length() < 50) {
            ra.addFlashAttribute("errorMessage", "Bình luận đánh giá sản phẩm bắt buộc phải có tối thiểu 50 ký tự! (Hiện tại: " + (content != null ? content.trim().length() : 0) + " ký tự)");
            if (orderId != null) {
                return "redirect:/customer/orders/detail/" + orderId;
            }
            return "redirect:/products/" + id + "#review-section";
        }

        try {
            reviewService.addReview(user.getId(), id, orderId, rating, content, imageUrl);
            ra.addFlashAttribute("successMessage", "Đánh giá, phản hồi thành công! Cảm ơn bạn đã đóng góp ý kiến.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }

        if (orderId != null) {
            return "redirect:/customer/orders/detail/" + orderId;
        }
        return "redirect:/products/" + id + "#review-section";
    }
}
