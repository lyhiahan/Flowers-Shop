package com.finalproject;

import com.finalproject.entity.Category;
import com.finalproject.entity.Order;
import com.finalproject.entity.Product;
import com.finalproject.entity.Store;
import com.finalproject.entity.User;
import com.finalproject.enums.OrderStatus;
import com.finalproject.enums.OrderType;
import com.finalproject.enums.Role;
import com.finalproject.repository.CategoryRepository;
import com.finalproject.repository.OrderRepository;
import com.finalproject.repository.ProductRepository;
import com.finalproject.repository.StoreRepository;
import com.finalproject.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Kiểm thử Use Case UC_029: Quản lý đơn hàng trong hệ thống (Orders management)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@SuppressWarnings("unused")
class AdminOrdersManagementUseCaseTests {

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
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User customer;
    private Store store;
    private Order pendingOrder;
    private Order deliveringOrder;
    private Order confirmedOrder;
    private Order deliveredOrder;

    @BeforeEach
    void setUp() {
        String encodedPass = passwordEncoder.encode("secret123");

        admin = userRepository.save(User.builder()
                .username("admin_uc29")
                .email("admin_uc29@example.com")
                .password(encodedPass)
                .fullName("Quản Trị Viên UC29")
                .role(Role.ADMIN)
                .isActive(true)
                .createdDate(LocalDate.now())
                .build());

        customer = userRepository.save(User.builder()
                .username("customer_uc29")
                .email("customer_uc29@example.com")
                .password(encodedPass)
                .fullName("Khách Hàng UC29")
                .role(Role.CUSTOMER)
                .isActive(true)
                .createdDate(LocalDate.now())
                .build());

        store = storeRepository.save(Store.builder()
                .name("Chi Nhánh Bến Thành UC29")
                .bio("Tiệm hoa tươi trung tâm")
                .address("123 Lê Lợi, Quận 1")
                .owner(admin)
                .eWallet(new BigDecimal("10000000"))
                .point(50)
                .status(true)
                .isOpen(true)
                .build());

        Category cat = categoryRepository.save(Category.builder()
                .name("Hoa Hồng Tươi UC29")
                .build());

        Product product = productRepository.save(Product.builder()
                .name("Bó Hoa Hồng Nhập Khẩu UC29")
                .category(cat)
                .store(store)
                .importPrice(new BigDecimal("200000"))
                .priceBuy(new BigDecimal("450000"))
                .priceRentPerDay(new BigDecimal("50000"))
                .depositFee(new BigDecimal("100000"))
                .sold(12)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build());

        // 1. Đơn hàng PENDING
        pendingOrder = orderRepository.save(Order.builder()
                .user(customer)
                .orderType(OrderType.BUY)
                .pickupStore(store)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("450000"))
                .shippingMethod("DELIVERY")
                .paymentMethod("COD")
                .paymentStatus("UNPAID")
                .recipientName("Người Nhận Chờ Tiếp Nhận")
                .recipientPhone("0901111111")
                .recipientAddress("456 Hai Bà Trưng, Q1")
                .createdAt(LocalDateTime.now())
                .build());

        // 2. Đơn hàng DELIVERING (Đã bàn giao cho đơn vị vận chuyển)
        deliveringOrder = orderRepository.save(Order.builder()
                .user(customer)
                .orderType(OrderType.BUY)
                .pickupStore(store)
                .status(OrderStatus.DELIVERING)
                .totalAmount(new BigDecimal("650000"))
                .shippingMethod("DELIVERY")
                .paymentMethod("COD")
                .paymentStatus("PAID")
                .recipientName("Người Nhận Đang Giao Hoa")
                .recipientPhone("0902222222")
                .recipientAddress("789 Nguyễn Huệ, Q1")
                .createdAt(LocalDateTime.now())
                .build());

        // 3. Đơn hàng CONFIRMED
        confirmedOrder = orderRepository.save(Order.builder()
                .user(customer)
                .orderType(OrderType.BUY)
                .pickupStore(store)
                .status(OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("500000"))
                .shippingMethod("DELIVERY")
                .paymentMethod("COD")
                .paymentStatus("PAID")
                .recipientName("Người Nhận Đã Xác Nhận")
                .recipientPhone("0903333333")
                .recipientAddress("321 Trần Hưng Đạo, Q1")
                .createdAt(LocalDateTime.now())
                .build());

        // 4. Đơn hàng DELIVERED
        deliveredOrder = orderRepository.save(Order.builder()
                .user(customer)
                .orderType(OrderType.BUY)
                .pickupStore(store)
                .status(OrderStatus.DELIVERED)
                .totalAmount(new BigDecimal("800000"))
                .shippingMethod("DELIVERY")
                .paymentMethod("COD")
                .paymentStatus("PAID")
                .recipientName("Người Nhận Đã Nhận Hoa")
                .recipientPhone("0904444444")
                .recipientAddress("654 Điện Biên Phủ, Q3")
                .createdAt(LocalDateTime.now())
                .build());
    }

    /**
     * Pre-condition: Chỉ người dùng có vai trò ADMIN mới được truy cập vào trang Quản lý đơn hàng.
     * Người dùng vai trò CUSTOMER sẽ bị từ chối truy cập (403 Forbidden).
     */
    @Test
    @WithMockUser(username = "customer_denied_uc29", roles = {"CUSTOMER"})
    void testPreCondition_CustomerDeniedAccessToAdminOrders() throws Exception {
        mockMvc.perform(get("/admin/orders-manage"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/orders-manage/" + deliveringOrder.getId()))
                .andExpect(status().isForbidden());
    }

    /**
     * Main Flow Bước 1 & 2:
     * 1. Vào trang Dashboard của Admin (/admin/dashboard)
     * 2. Chọn Orders bên thanh sidebar (id="sidebarOrdersLink" dẫn tới /admin/orders-manage)
     */
    @Test
    @WithMockUser(username = "admin_uc29", roles = {"ADMIN"})
    void testStep1And2_NavigateFromDashboardToOrders() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(content().string(containsString("id=\"sidebarOrdersLink\"")))
                .andExpect(content().string(containsString("href=\"/admin/orders-manage\"")));
    }

    /**
     * Main Flow Bước 3:
     * 3. Chọn All (hoặc Delivery Service) để xem tất cả (hoặc các đơn hàng đang vận chuyển)
     */
    @Test
    @WithMockUser(username = "admin_uc29", roles = {"ADMIN"})
    void testStep3_SelectAllAndDeliveryServiceTabs() throws Exception {
        // Tab All: Hiển thị tất cả các đơn hàng
        mockMvc.perform(get("/admin/orders-manage")
                        .param("tab", "ALL"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/orders-manage"))
                .andExpect(model().attribute("currentTab", "ALL"))
                .andExpect(content().string(containsString("id=\"tabAllOrders\"")))
                .andExpect(content().string(containsString("id=\"tabDeliveryServiceOrders\"")))
                .andExpect(content().string(containsString("Người Nhận Chờ Tiếp Nhận")))
                .andExpect(content().string(containsString("Người Nhận Đang Giao Hoa")));

        // Tab Delivery Service: Chỉ hiển thị các đơn hàng đang vận chuyển (status == DELIVERING)
        mockMvc.perform(get("/admin/orders-manage")
                        .param("tab", "DELIVERY_SERVICE"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/orders-manage"))
                .andExpect(model().attribute("currentTab", "DELIVERY_SERVICE"))
                .andExpect(content().string(containsString("id=\"tabAllOrders\"")))
                .andExpect(content().string(containsString("id=\"tabDeliveryServiceOrders\"")))
                .andExpect(content().string(containsString("Người Nhận Đang Giao Hoa")))
                .andExpect(content().string(not(containsString("Người Nhận Chờ Tiếp Nhận"))));
    }

    /**
     * Main Flow Bước 4:
     * 4. Hiển thị bảng danh sách đơn hàng (id="ordersTable")
     * Post-condition (thành công): Hiển thị bảng danh sách tất cả các đơn hàng trong hệ thống
     */
    @Test
    @WithMockUser(username = "admin_uc29", roles = {"ADMIN"})
    void testStep4_DisplayOrdersTable() throws Exception {
        mockMvc.perform(get("/admin/orders-manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/orders-manage"))
                .andExpect(model().attributeExists("orders"))
                .andExpect(model().attributeExists("totalPages"))
                .andExpect(model().attributeExists("totalElements"))
                .andExpect(model().attributeExists("currentPage"))
                .andExpect(content().string(containsString("id=\"ordersTable\"")))
                .andExpect(content().string(containsString("Quản lý đơn hàng")))
                .andExpect(content().string(containsString("id=\"btnViewOrder_" + deliveringOrder.getId() + "\"")))
                .andExpect(content().string(containsString("Người Nhận Đang Giao Hoa")));
    }

    /**
     * Main Flow Bước 5:
     * 5. Chọn Next (hoặc Previous, hoặc trang bất kỳ) để xem trang tiếp theo (hoặc trước đó, hoặc trang tương ứng)
     */
    @Test
    @WithMockUser(username = "admin_uc29", roles = {"ADMIN"})
    void testStep5_PaginationNextPreviousAndDirectPage() throws Exception {
        // Truy cập trang đầu tiên với kích thước trang là 2 (tổng cộng 4 đơn hàng tạo ra ít nhất 2 trang)
        mockMvc.perform(get("/admin/orders-manage")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/orders-manage"))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(content().string(containsString("id=\"ordersPagination\"")))
                .andExpect(content().string(containsString("id=\"btnNextPage\"")))
                .andExpect(content().string(containsString("id=\"btnPrevPage\"")))
                .andExpect(content().string(containsString("id=\"pageBtn_1\"")))
                .andExpect(content().string(containsString("id=\"pageBtn_2\"")));

        // Bấm Next (chọn trang tiếp theo: page=1)
        mockMvc.perform(get("/admin/orders-manage")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/orders-manage"))
                .andExpect(model().attribute("currentPage", 1))
                .andExpect(content().string(containsString("id=\"ordersPagination\"")));
    }

    /**
     * Main Flow Bước 6 & 7:
     * 6. Chọn nút xem chi tiết
     * 7. Hiển thị trang chi tiết đơn hàng tương ứng
     * Bước 8: Chọn cập nhật trạng thái đơn hàng ở phần dropdown (nút này sẽ tồn tại với các đơn hàng đã bàn giao cho đơn vị vận chuyển của hệ thống)
     * Bước 9: Hiển thị thông báo xác nhận
     * Alternative 10a & 11a: Bấm nút Cancel, hủy thao tác
     */
    @Test
    @WithMockUser(username = "admin_uc29", roles = {"ADMIN"})
    void testStep6To9AndAlternative10a_ViewOrderDetailAndCheckStatusControls() throws Exception {
        // Đơn hàng DELIVERING đã bàn giao cho đơn vị vận chuyển -> Có dropdown và nút cập nhật trạng thái
        mockMvc.perform(get("/admin/orders-manage/" + deliveringOrder.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/order-detail"))
                .andExpect(model().attribute("isHandedOverToDelivery", true))
                .andExpect(model().attributeExists("availableStatuses"))
                .andExpect(content().string(containsString("id=\"dropdownOrderStatus\"")))
                .andExpect(content().string(containsString("id=\"btnUpdateStatus\"")))
                .andExpect(content().string(containsString("id=\"confirmStatusModal\"")))
                .andExpect(content().string(containsString("id=\"btnConfirm\"")))
                .andExpect(content().string(containsString("id=\"btnCancel\"")));
    }

    /**
     * Main Flow Bước 10 & 11:
     * 10. Bấm nút Confirm
     * 11. Thông báo thành công
     */
    @Test
    @WithMockUser(username = "admin_uc29", roles = {"ADMIN"})
    void testStep10And11_UpdateOrderStatusConfirmSuccess() throws Exception {
        mockMvc.perform(post("/admin/orders-manage/update-status/" + deliveringOrder.getId())
                        .param("status", OrderStatus.DELIVERED.name())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/orders-manage/" + deliveringOrder.getId()))
                .andExpect(flash().attributeExists("successMessage"));

        Order updated = orderRepository.findById(deliveringOrder.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    /**
     * Post-condition (thất bại): Nếu thất bại hoặc có lỗi tải dữ liệu, thông báo lỗi được hiển thị.
     */
    @Test
    @WithMockUser(username = "admin_uc29", roles = {"ADMIN"})
    void testPostCondition_ErrorNotificationWhenDataLoadFails() throws Exception {
        mockMvc.perform(get("/admin/orders-manage")
                        .flashAttr("dataLoadError", true)
                        .flashAttr("errorMessage", "Không thể tải danh sách đơn hàng. Vui lòng thử lại!"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"dataLoadAlert\"")))
                .andExpect(content().string(containsString("Không thể tải danh sách đơn hàng. Vui lòng thử lại!")));
    }
}
