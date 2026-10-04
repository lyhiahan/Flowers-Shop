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
    private final com.finalproject.repository.StoreInventoryRepository inventories;

    @GetMapping({"/product/{id}", "/products/{id}"})
    public String viewProductDetail(@PathVariable("id") Long id, Principal principal, Model model) {
        Product product = productService.getProductById(id);

        if (!com.finalproject.service.ProductAvailability.isPublic(product)) throw new com.finalproject.exception.ResourceNotFoundException("Sản phẩm không còn được bán.");

        // Tăng lượt xem
        product.setViewCount(product.getViewCount() != null ? product.getViewCount() + 1 : 1);
        productRepository.save(product);

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
                canReview = reviewService.canReview(u.getId(), id);
                eligibleOrderId = reviewService.getEligibleOrderId(u.getId(), id);
                try {
                    model.addAttribute("cartItemCount", cartService.getCartItemCount(principal.getName()));
                } catch (Exception ignored) {}
            }
        }

        model.addAttribute("product", product);
        int availableStock = product.getStore() == null ? 0 : inventories
                .findByStoreIdAndProductId(product.getStore().getId(), product.getId())
                .map(stock -> Math.max(0, stock.getQuantity())).orElse(0);
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
        model.addAttribute("relatedProducts", product.getCategory() != null 
                ? productRepository.findByCategoryId(product.getCategory().getId()).stream().filter(com.finalproject.service.ProductAvailability::isPublic).limit(4).toList()
                : java.util.List.of());

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

        // Kiểm tra quyền đánh giá: chỉ khách hàng đã đặt hàng và nhận được hàng rồi mới được đánh giá
        if (!reviewService.canReview(user.getId(), id)) {
            ra.addFlashAttribute("errorMessage", "Chức năng đánh giá sản phẩm chỉ được thực hiện khi và chỉ khi khách hàng đã đặt hàng và nhận được hàng thành công!");
            return "redirect:/product/" + id + "#review-section";
        }

        // Kiểm tra độ dài bình luận tối thiểu 50 ký tự theo yêu cầu đề cương
        if (content == null || content.trim().length() < 50) {
            ra.addFlashAttribute("errorMessage", "Bình luận đánh giá sản phẩm bắt buộc phải có tối thiểu 50 ký tự! (Hiện tại: " + (content != null ? content.trim().length() : 0) + " ký tự)");
            if (orderId != null) {
                return "redirect:/customer/orders/detail/" + orderId;
            }
            return "redirect:/product/" + id + "#review-section";
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
        return "redirect:/product/" + id + "#review-section";
    }
}
