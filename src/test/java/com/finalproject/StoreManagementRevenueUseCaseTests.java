package com.finalproject;

import com.finalproject.entity.*;
import com.finalproject.enums.OrderStatus;
import com.finalproject.enums.OrderType;
import com.finalproject.enums.Role;
import com.finalproject.repository.*;
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
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Kiểm thử Use Case UC_024: Thống kê doanh thu (Revenue statistics)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@SuppressWarnings("unused")
class StoreManagementRevenueUseCaseTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;
    @Autowired private OrderDetailRepository orderDetails;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User vendor;
    private User admin;
    private Store store;
    private Product product1;
    private Product product2;
    private Order order1;
    private Order order2;

    @BeforeEach
    void setUp() {
        String encodedPass = passwordEncoder.encode("secret123");

        // Vendor
        vendor = userRepository.save(User.builder()
                .username("vendor_uc24")
                .email("vendor_uc24@example.com")
                .password(encodedPass)
                .fullName("Chủ Shop UC24")
                .role(Role.MANAGER)
                .isActive(true)
                .build());

        // Admin
        admin = userRepository.save(User.builder()
                .username("admin_uc24")
                .email("admin_uc24@example.com")
                .password(encodedPass)
                .fullName("Quản Trị Viên UC24")
                .role(Role.ADMIN)
                .isActive(true)
                .build());

        // Store
        store = storeRepository.save(Store.builder()
                .name("Chi Nhánh Hoa Bến Thành UC24")
                .address("123 Lê Lợi, Q1, TP.HCM")
                .phone("0909123456")
                .owner(vendor)
                .eWallet(new BigDecimal("5000000"))
                .status(true)
                .build());

        vendor.setStore(store);
        userRepository.save(vendor);

        // Category & Products
        Category cat = categoryRepository.save(Category.builder()
                .name("Hoa Tươi Sài Gòn UC24")
                .build());

        product1 = productRepository.save(Product.builder()
                .name("Bó Hoa Hồng Đỏ UC24")
                .store(store)
                .category(cat)
                .importPrice(new BigDecimal("200000"))
                .priceBuy(new BigDecimal("300000"))
                .priceRentPerDay(new BigDecimal("50000"))
                .depositFee(new BigDecimal("100000"))
                .sold(25)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build());

        product2 = productRepository.save(Product.builder()
                .name("Giỏ Hoa Hướng Dương UC24")
                .store(store)
                .category(cat)
                .importPrice(new BigDecimal("300000"))
                .priceBuy(new BigDecimal("450000"))
                .priceRentPerDay(new BigDecimal("70000"))
                .depositFee(new BigDecimal("150000"))
                .sold(15)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build());

        // Orders
        order1 = orderRepository.save(Order.builder()
                .user(vendor)
                .pickupStore(store)
                .orderType(OrderType.BUY)
                .status(OrderStatus.COMPLETED)
                .paymentStatus("PAID")
                .totalAmount(new BigDecimal("600000"))
                .recipientName("Khách Hàng A")
                .recipientPhone("0987654321")
                .createdAt(LocalDateTime.now())
                .build());

        var detail = orderDetails.save(OrderDetail.builder().order(order1).product(product1).quantity(2).unitPrice(new BigDecimal("300000")).orderType(OrderType.BUY).build());
        order1.setOrderDetails(new java.util.ArrayList<>(java.util.List.of(detail)));

        order2 = orderRepository.save(Order.builder()
                .user(vendor)
                .pickupStore(store)
                .orderType(OrderType.BUY)
                .status(OrderStatus.PENDING)
                .paymentStatus("UNPAID")
                .totalAmount(new BigDecimal("450000"))
                .recipientName("Khách Hàng B")
                .recipientPhone("0912345678")
                .createdAt(LocalDateTime.now())
                .build());
    }

    /**
     * Main Flow Bước 1-6:
     * - Vào hệ thống, chuyển đến dashboard
     * - Hiển thị tổng số sản phẩm, đơn hàng (KPI cards)
     */
    @Test
    @WithMockUser(username = "vendor_uc24", roles = {"MANAGER"})
    void testStep1To6_DisplayTotalProductsAndOrders() throws Exception {
        mockMvc.perform(get("/manager/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/dashboard"))
                .andExpect(model().attributeExists("store"))
                .andExpect(model().attribute("totalProductsCount", 2L))
                .andExpect(model().attribute("totalOrdersCount", 2L))
                .andExpect(model().attributeExists("chartLabels"))
                .andExpect(model().attributeExists("chartValues"))
                .andExpect(content().string(containsString("id=\"kpi-total-products\"")))
                .andExpect(content().string(containsString("id=\"totalProductsCount\"")))
                .andExpect(content().string(containsString("id=\"kpi-total-orders\"")))
                .andExpect(content().string(containsString("id=\"totalOrdersCount\"")))
                .andExpect(content().string(containsString("id=\"revenueTrendChart\"")));
    }

    /**
     * Main Flow Bước 7:
     * - Tùy chọn xem thống kê theo đơn hàng, sản phẩm, thời gian (giờ, ngày, tháng, năm), loại biểu đồ (đường, cột, tròn)
     */
    @Test
    @WithMockUser(username = "vendor_uc24", roles = {"MANAGER"})
    void testStep7_FilterByCriteriaTimeRangeAndChartType() throws Exception {
        // 1. Xem theo Đơn hàng + Theo giờ + Biểu đồ Cột
        mockMvc.perform(get("/manager/dashboard")
                        .param("criteria", "orders")
                        .param("timeRange", "hour")
                        .param("chartType", "bar"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("criteria", "orders"))
                .andExpect(model().attribute("timeRange", "hour"))
                .andExpect(model().attribute("chartType", "bar"))
                .andExpect(model().attributeExists("chartOrderValues"))
                .andExpect(content().string(containsString("id=\"filterCriteriaOrders\"")))
                .andExpect(content().string(containsString("id=\"filterTimeHour\"")))
                .andExpect(content().string(containsString("id=\"chartTypeBar\"")));

        // 2. Xem theo Sản phẩm + Theo tháng + Biểu đồ Tròn
        mockMvc.perform(get("/manager/dashboard")
                        .param("criteria", "products")
                        .param("timeRange", "month")
                        .param("chartType", "pie"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("criteria", "products"))
                .andExpect(model().attribute("timeRange", "month"))
                .andExpect(model().attribute("chartType", "pie"))
                .andExpect(model().attributeExists("chartProductValues"))
                .andExpect(content().string(containsString("id=\"filterCriteriaProducts\"")))
                .andExpect(content().string(containsString("id=\"filterTimeMonth\"")))
                .andExpect(content().string(containsString("id=\"chartTypePie\"")));

        // 3. Xem theo Doanh thu + Theo năm + Biểu đồ Đường
        mockMvc.perform(get("/manager/dashboard")
                        .param("criteria", "revenue")
                        .param("timeRange", "year")
                        .param("chartType", "line"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("criteria", "revenue"))
                .andExpect(model().attribute("timeRange", "year"))
                .andExpect(model().attribute("chartType", "line"))
                .andExpect(model().attributeExists("chartRevenueValues"))
                .andExpect(content().string(containsString("id=\"filterCriteriaRevenue\"")))
                .andExpect(content().string(containsString("id=\"filterTimeYear\"")))
                .andExpect(content().string(containsString("id=\"chartTypeLine\"")));
    }

    /**
     * Main Flow Bước 8 & Post-condition:
     * - Hiển thị bên cạnh biểu đồ là Top sản phẩm bán chạy, Top đơn hàng mới nhất (lên đến 1000 mục)
     */
    @Test
    @WithMockUser(username = "vendor_uc24", roles = {"MANAGER"})
    void testStep8_TopSellingProductsAndRecentOrdersCards() throws Exception {
        mockMvc.perform(get("/manager/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("topProducts"))
                .andExpect(model().attributeExists("recentOrders"))
                .andExpect(content().string(containsString("id=\"topSellingProductsCard\"")))
                .andExpect(content().string(containsString("id=\"topRecentOrdersCard\"")))
                .andExpect(content().string(containsString("Bó Hoa Hồng Đỏ UC24")))
                .andExpect(content().string(containsString("#HD" + order1.getId())));
    }

    /**
     * Alternative Flow 1-5a:
     * - Quản trị viên (Admin) vào trang Dashboard của Admin
     * - Thống kê doanh thu, tổng số sản phẩm, đơn hàng toàn hệ thống
     */
    @Test
    @WithMockUser(username = "admin_uc24", roles = {"ADMIN"})
    void testAlternative1_5a_AdminDashboardRevenueStatistics() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("totalProductsCount"))
                .andExpect(model().attributeExists("totalOrdersCount"))
                .andExpect(model().attributeExists("totalRevenue"))
                .andExpect(model().attributeExists("recentOrders"))
                .andExpect(model().attributeExists("topProducts"))
                .andExpect(content().string(containsString("Trung tâm quản trị hệ thống")));
    }

    /**
     * Main Flow Bước 2-4:
     * - Chọn chức năng Shop Manager (/manager/stores)
     * - Nút Go to dashboard của một cửa hàng chuyển đến vendor dashboard
     */
    @Test
    @WithMockUser(username = "vendor_uc24", roles = {"MANAGER"})
    void testMainFlow_GoToDashboardButton() throws Exception {
        // Vào trang quản lý cửa hàng cá nhân
        mockMvc.perform(get("/manager/stores"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("btnGoDashboard_" + store.getId())));

        // Bấm nút Go to dashboard
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/manager/stores/switch/" + store.getId()).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/dashboard"));
    }
}
