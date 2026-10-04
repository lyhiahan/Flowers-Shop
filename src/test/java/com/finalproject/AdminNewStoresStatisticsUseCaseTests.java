package com.finalproject;

import com.finalproject.entity.Store;
import com.finalproject.entity.User;
import com.finalproject.enums.Role;
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

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Kiểm thử Use Case UC_026: Thống kê cửa hàng mới (New stores statistics)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@SuppressWarnings("unused")
class AdminNewStoresStatisticsUseCaseTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User vendor;
    private Store storeTop1;
    private Store storeTop2;
    private Store storeTop3;

    @BeforeEach
    void setUp() {
        String encodedPass = passwordEncoder.encode("secret123");

        admin = userRepository.save(User.builder()
                .username("admin_uc26")
                .email("admin_uc26@example.com")
                .password(encodedPass)
                .fullName("Quản Trị Viên UC26")
                .role(Role.ADMIN)
                .isActive(true)
                .build());

        vendor = userRepository.save(User.builder()
                .username("vendor_uc26")
                .email("vendor_uc26@example.com")
                .password(encodedPass)
                .fullName("Chủ Cửa Hàng UC26")
                .role(Role.VENDOR)
                .isActive(true)
                .build());

        storeTop1 = storeRepository.save(Store.builder()
                .name("Cửa Hàng Hoa Sen Vàng")
                .address("123 Lê Lợi, Q1, TP.HCM")
                .phone("0901111111")
                .owner(vendor)
                .point(950)
                .rating(5.0)
                .status(true)
                .isOpen(true)
                .createdAt(LocalDateTime.now())
                .build());

        storeTop2 = storeRepository.save(Store.builder()
                .name("Cửa Hàng Hoa Hồng Đỏ")
                .address("456 Nguyễn Huệ, Q1, TP.HCM")
                .phone("0902222222")
                .owner(vendor)
                .point(720)
                .rating(4.8)
                .status(true)
                .isOpen(true)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build());

        storeTop3 = storeRepository.save(Store.builder()
                .name("Cửa Hàng Hoa Hướng Dương")
                .address("789 Cách Mạng Tháng 8, Q10, TP.HCM")
                .phone("0903333333")
                .owner(vendor)
                .point(450)
                .rating(4.6)
                .status(true)
                .isOpen(true)
                .createdAt(LocalDateTime.now().minusDays(2))
                .build());
    }

    /**
     * Main Flow Bước 1 & 2:
     * 1. Vào trang Dashboard của Admin (/admin hoặc /admin/dashboard)
     * 2. Hiển thị tổng số cửa hàng của hệ thống (KPI card kpi-total-stores & totalStoresCount)
     */
    @Test
    @WithMockUser(username = "admin_uc26", roles = {"ADMIN"})
    void testStep1And2_DisplayTotalStoresOnAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("totalStores"))
                .andExpect(model().attributeExists("totalStoresCount"))
                .andExpect(content().string(containsString("id=\"kpi-total-stores\"")))
                .andExpect(content().string(containsString("id=\"totalStoresCount\"")))
                .andExpect(content().string(containsString("id=\"btnViewStoresChart\"")));
    }

    /**
     * Main Flow Bước 3:
     * 3. Bấm vào xem biểu đồ (liên kết #btnViewStoresChart dẫn tới khu vực biểu đồ #storesChartSection)
     */
    @Test
    @WithMockUser(username = "admin_uc26", roles = {"ADMIN"})
    void testStep3_ViewStoresChartSectionExists() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"storesChartSection\"")))
                .andExpect(content().string(containsString("id=\"newStoreChartCard\"")))
                .andExpect(content().string(containsString("id=\"adminStoresChart\"")));
    }

    /**
     * Main Flow Bước 4:
     * 4. Chọn loại biểu đồ (đường, cột, tròn), chọn thời gian (giờ, ngày, tháng, năm)
     */
    @Test
    @WithMockUser(username = "admin_uc26", roles = {"ADMIN"})
    void testStep4_SelectChartTypeAndTimeRange() throws Exception {
        // Kiểm tra điều khiển loại biểu đồ (storeLineBtn, storeBarBtn, storePieBtn)
        // và điều khiển thời gian (storeTimeHour, storeTimeDay, storeTimeMonth, storeTimeYear)
        mockMvc.perform(get("/admin/dashboard")
                        .param("timeframe", "month")
                        .param("storeChartType", "line"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("timeframe", "month"))
                .andExpect(model().attribute("storeChartType", "line"))
                .andExpect(content().string(containsString("id=\"storeLineBtn\"")))
                .andExpect(content().string(containsString("id=\"storeBarBtn\"")))
                .andExpect(content().string(containsString("id=\"storePieBtn\"")))
                .andExpect(content().string(containsString("id=\"storeTimeHour\"")))
                .andExpect(content().string(containsString("id=\"storeTimeDay\"")))
                .andExpect(content().string(containsString("id=\"storeTimeMonth\"")))
                .andExpect(content().string(containsString("id=\"storeTimeYear\"")));
    }

    /**
     * Main Flow Bước 5:
     * 5. Hiển thị bên cạnh biểu đồ là Top cửa hàng điểm cao nhất hệ thống
     * Post-condition: Thống kê tổng số cửa hàng, vẽ biểu đồ cho 1000 cửa hàng mới nhất, hiển thị top điểm
     */
    @Test
    @WithMockUser(username = "admin_uc26", roles = {"ADMIN"})
    void testStep5_DisplayTopPointStoresBesideChart() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("topPointStores"))
                .andExpect(model().attributeExists("recentStores"))
                .andExpect(content().string(containsString("id=\"topPointStoresCard\"")))
                .andExpect(content().string(containsString("Top Cửa Hàng Điểm Cao Nhất")))
                .andExpect(content().string(containsString("Cửa Hàng Hoa Sen Vàng")))
                .andExpect(content().string(containsString("950 pts")))
                .andExpect(content().string(containsString("Cửa Hàng Hoa Hồng Đỏ")))
                .andExpect(content().string(containsString("720 pts")));
    }

    /**
     * Pre-condition: Chỉ người dùng có vai trò ADMIN mới được truy cập Dashboard Admin.
     * Người dùng vai trò CUSTOMER hoặc VENDOR không có quyền truy cập vào /admin/dashboard (403 Forbidden).
     */
    @Test
    @WithMockUser(username = "customer_denied", roles = {"CUSTOMER"})
    void testPreCondition_CustomerDeniedAccessToAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }
}
