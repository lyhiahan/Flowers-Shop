package com.finalproject;

import com.finalproject.entity.Category;
import com.finalproject.entity.Product;
import com.finalproject.entity.Store;
import com.finalproject.entity.StoreInventory;
import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CartBadgeAjaxUpdateTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StoreInventoryRepository storeInventoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Store store;
    private Product product;

    @BeforeEach
    void setup() {
        userRepository.save(User.builder()
                .username("test_cart_badge_user")
                .password(passwordEncoder.encode("123456"))
                .email("cartbadge@flower.vn")
                .fullName("Khách Hàng Test Giỏ")
                .role(Role.USER)
                .isActive(true)
                .build());

        store = storeRepository.save(Store.builder()
                .name("Chi Nhánh Hoa Bến Thành")
                .address("123 Lê Lợi, Q1")
                .phone("0909123456")
                .status(true)
                .isOpen(true)
                .eWallet(new BigDecimal("5000000"))
                .build());

        Category cat = categoryRepository.save(Category.builder()
                .name("Hoa Tươi Sài Gòn")
                .isDeleted(false)
                .build());

        product = productRepository.save(Product.builder()
                .name("Bó Hoa Hướng Dương Mặt Trời")
                .store(store)
                .category(cat)
                .priceBuy(new BigDecimal("350000"))
                .importPrice(new BigDecimal("200000"))
                .depositFee(new BigDecimal("100000"))
                .priceRentPerDay(new BigDecimal("50000"))
                .isSelling(true)
                .isActive(true)
                .isDeleted(false)
                .build());

        storeInventoryRepository.save(StoreInventory.builder()
                .store(store)
                .product(product)
                .quantity(100)
                .build());
    }

    @Test
    @DisplayName("1. Giỏ trống -> thêm 1 -> API trả về count=1 ngay lập tức")
    @WithMockUser(username = "test_cart_badge_user", roles = {"USER"})
    void emptyCart_addOneItem_returnsCountOne() throws Exception {
        mockMvc.perform(post("/cart/add")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .with(csrf())
                        .param("productId", product.getId().toString())
                        .param("storeId", store.getId().toString())
                        .param("orderType", "BUY")
                        .param("quantity", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    @DisplayName("2. Giỏ có 2 -> thêm 1 -> API trả về count=3")
    @WithMockUser(username = "test_cart_badge_user", roles = {"USER"})
    void cartHasTwo_addOneItem_returnsCountThree() throws Exception {
        // Khởi tạo giỏ có 2 sản phẩm
        mockMvc.perform(post("/cart/add")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .with(csrf())
                        .param("productId", product.getId().toString())
                        .param("storeId", store.getId().toString())
                        .param("orderType", "BUY")
                        .param("quantity", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));

        // Thêm tiếp 1 sản phẩm
        mockMvc.perform(post("/cart/add")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .with(csrf())
                        .param("productId", product.getId().toString())
                        .param("storeId", store.getId().toString())
                        .param("orderType", "BUY")
                        .param("quantity", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    @DisplayName("3. Giỏ có 2 -> chọn số lượng 3 rồi thêm -> API trả về count=5")
    @WithMockUser(username = "test_cart_badge_user", roles = {"USER"})
    void cartHasTwo_addThreeItems_returnsCountFive() throws Exception {
        // Khởi tạo giỏ có 2 sản phẩm
        mockMvc.perform(post("/cart/add")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .with(csrf())
                        .param("productId", product.getId().toString())
                        .param("storeId", store.getId().toString())
                        .param("orderType", "BUY")
                        .param("quantity", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));

        // Chọn số lượng 3 rồi thêm
        mockMvc.perform(post("/cart/add")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .with(csrf())
                        .param("productId", product.getId().toString())
                        .param("storeId", store.getId().toString())
                        .param("orderType", "BUY")
                        .param("quantity", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.count").value(5));
    }

    @Test
    @DisplayName("4. Trang chi tiết sản phẩm chứa các ID, class và script cập nhật số nhỏ màu đỏ")
    @WithMockUser(username = "test_cart_badge_user", roles = {"USER"})
    void productDetailPage_containsCartBadgeElementsAndScript() throws Exception {
        mockMvc.perform(get("/product/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"headerCartLink\"")))
                .andExpect(content().string(containsString("id=\"headerCartBadge\"")))
                .andExpect(content().string(containsString("cart-badge-pill")))
                .andExpect(content().string(containsString("function updateCartBadge")));
    }

    @Test
    @DisplayName("5. Thêm thất bại (sản phẩm không tồn tại) -> Trả về lỗi 400, không cập nhật count")
    @WithMockUser(username = "test_cart_badge_user", roles = {"USER"})
    void addFailed_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/cart/add")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .with(csrf())
                        .param("productId", "999999")
                        .param("storeId", store.getId().toString())
                        .param("orderType", "BUY")
                        .param("quantity", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
