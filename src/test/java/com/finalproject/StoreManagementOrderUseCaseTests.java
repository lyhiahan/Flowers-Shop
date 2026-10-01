package com.finalproject;

import com.finalproject.entity.Order;
import com.finalproject.entity.Store;
import com.finalproject.entity.User;
import com.finalproject.enums.OrderStatus;
import com.finalproject.enums.Role;
import com.finalproject.repository.OrderRepository;
import com.finalproject.repository.StoreRepository;
import com.finalproject.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Kiểm thử Use Case UC_022: Quản lý đơn hàng của cửa hàng (Store Management – Order)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StoreManagementOrderUseCaseTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private OrderRepository orderRepository;

    private User vendor;
    private Store store;
    private Order processingOrder;
    private Order processedOrder;

    @BeforeEach
    void setUp() {
        // Tạo vendor
        vendor = userRepository.save(User.builder()
                .username("vendor_uc22")
                .email("vendor_uc22@example.com")
                .password("encoded_pass")
                .fullName("Vendor UC22")
                .phone("0901112222")
                .role(Role.MANAGER)
                .isActive(true)
                .build());

        // Tạo store
        store = storeRepository.save(Store.builder()
                .name("Chi Nhánh Hoa UC22")
                .address("123 Hoa Hồng, Q1")
                .phone("0901112222")
                .owner(vendor)
                .isOpen(true)
                .status(true)
                .rating(5.0)
                .eWallet(BigDecimal.ZERO)
                .build());

        vendor.setStore(store);
        userRepository.save(vendor);

        // Tạo đơn hàng đang xử lý (PENDING)
        processingOrder = orderRepository.save(Order.builder()
                .user(vendor)
                .orderType(com.finalproject.enums.OrderType.BUY)
                .pickupStore(store)
                .recipientName("Khách Hàng A")
                .recipientPhone("0988776655")
                .recipientAddress("456 Lê Lợi, Q1")
                .totalAmount(BigDecimal.valueOf(500000))
                .status(OrderStatus.PENDING)
                .paymentMethod("COD")
                .paymentStatus("UNPAID")
                .shippingMethod("DELIVERY")
                .createdAt(LocalDateTime.now())
                .build());

        // Tạo đơn hàng đã xử lý (COMPLETED)
        processedOrder = orderRepository.save(Order.builder()
                .user(vendor)
                .orderType(com.finalproject.enums.OrderType.BUY)
                .pickupStore(store)
                .recipientName("Khách Hàng B")
                .recipientPhone("0977665544")
                .recipientAddress("789 Hai Bà Trưng, Q1")
                .totalAmount(BigDecimal.valueOf(800000))
                .status(OrderStatus.COMPLETED)
                .paymentMethod("COD")
                .paymentStatus("PAID")
                .shippingMethod("DELIVERY")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build());
    }

    /**
     * Test Main Flow bước 6, 7, 8: Vào trang orders và lọc Processing orders
     */
    @Test
    @WithMockUser(username = "vendor_uc22", roles = {"MANAGER"})
    void testListProcessingOrders() throws Exception {
        mockMvc.perform(get("/manager/orders").param("tab", "processing"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/orders"))
                .andExpect(model().attributeExists("orders", "processingCount", "processedCount", "totalPages"))
                .andExpect(content().string(containsString("tabProcessingOrders")))
                .andExpect(content().string(containsString("tabProcessedOrders")))
                .andExpect(content().string(containsString("Khách Hàng A")));
    }

    /**
     * Test Main Flow bước 8: Lọc Processed orders
     */
    @Test
    @WithMockUser(username = "vendor_uc22", roles = {"MANAGER"})
    void testListProcessedOrders() throws Exception {
        mockMvc.perform(get("/manager/orders").param("tab", "processed"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/orders"))
                .andExpect(content().string(containsString("Khách Hàng B")));
    }

    /**
     * Test Main Flow bước 9: Phân trang (Next, Previous, số trang)
     */
    @Test
    @WithMockUser(username = "vendor_uc22", roles = {"MANAGER"})
    void testPaginationOnOrders() throws Exception {
        mockMvc.perform(get("/manager/orders")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/orders"))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(content().string(containsString("btnNextPage")))
                .andExpect(content().string(containsString("btnPrevPage")));
    }

    /**
     * Test Main Flow bước 10, 11: Xem chi tiết đơn hàng
     */
    @Test
    @WithMockUser(username = "vendor_uc22", roles = {"MANAGER"})
    void testViewOrderDetail() throws Exception {
        mockMvc.perform(get("/manager/orders/" + processingOrder.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/order-detail"))
                .andExpect(model().attribute("isProcessing", true))
                .andExpect(model().attributeExists("availableStatuses"))
                .andExpect(content().string(containsString("dropdownOrderStatus")))
                .andExpect(content().string(containsString("btnUpdateStatus")))
                .andExpect(content().string(containsString("confirmStatusModal")))
                .andExpect(content().string(containsString("btnConfirm")))
                .andExpect(content().string(containsString("btnCancel")));
    }

    /**
     * Test Main Flow bước 12, 13, 14, 15: Cập nhật trạng thái đơn hàng (PENDING -> CONFIRMED)
     */
    @Test
    @WithMockUser(username = "vendor_uc22", roles = {"MANAGER"})
    void testUpdateOrderStatusSuccess() throws Exception {
        mockMvc.perform(post("/manager/orders/update-status/" + processingOrder.getId())
                        .param("status", OrderStatus.CONFIRMED.name())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/orders/" + processingOrder.getId()))
                .andExpect(flash().attributeExists("successMessage"));

        Order updated = orderRepository.findById(processingOrder.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }
}
