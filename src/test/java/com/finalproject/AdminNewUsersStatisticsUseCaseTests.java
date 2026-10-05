package com.finalproject;

import com.finalproject.entity.User;
import com.finalproject.enums.Role;
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

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Kiểm thử Use Case UC_025: Thống kê người dùng mới (New users statistics)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@SuppressWarnings("unused")
class AdminNewUsersStatisticsUseCaseTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User userTop1;
    private User userTop2;
    private User userTop3;

    @BeforeEach
    void setUp() {
        String encodedPass = passwordEncoder.encode("secret123");

        admin = userRepository.save(User.builder()
                .username("admin_uc25")
                .email("admin_uc25@example.com")
                .password(encodedPass)
                .fullName("Quản Trị Viên UC25")
                .role(Role.ADMIN)
                .point(500)
                .isActive(true)
                .createdDate(LocalDate.now())
                .build());

        userTop1 = userRepository.save(User.builder()
                .username("customer_top1_uc25")
                .email("top1_uc25@example.com")
                .password(encodedPass)
                .fullName("Khách Hàng Vip Nhất")
                .role(Role.USER)
                .point(9800)
                .isActive(true)
                .createdDate(LocalDate.now())
                .build());

        userTop2 = userRepository.save(User.builder()
                .username("customer_top2_uc25")
                .email("top2_uc25@example.com")
                .password(encodedPass)
                .fullName("Khách Hàng Vip Nhì")
                .role(Role.USER)
                .point(7500)
                .isActive(true)
                .createdDate(LocalDate.now().minusDays(1))
                .build());

        userTop3 = userRepository.save(User.builder()
                .username("customer_top3_uc25")
                .email("top3_uc25@example.com")
                .password(encodedPass)
                .fullName("Khách Hàng Vip Ba")
                .role(Role.USER)
                .point(4200)
                .isActive(true)
                .createdDate(LocalDate.now().minusDays(2))
                .build());
    }

    /**
     * Main Flow Bước 1 & 2:
     * 1. Vào trang Dashboard của Admin (/admin hoặc /admin/dashboard)
     * 2. Hiển thị tổng số người dùng của hệ thống (KPI card kpi-total-users & totalUsersCount)
     */
    @Test
    @WithMockUser(username = "admin_uc25", roles = {"ADMIN"})
    void testStep1And2_DisplayTotalUsersOnAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("totalUsers"))
                .andExpect(model().attributeExists("totalUsersCount"))
                .andExpect(content().string(containsString("id=\"kpi-total-users\"")))
                .andExpect(content().string(containsString("id=\"totalUsersCount\"")))
                .andExpect(content().string(containsString("id=\"btnViewUsersChart\"")));
    }

    /**
     * Main Flow Bước 3:
     * 3. Bấm vào xem biểu đồ (liên kết #btnViewUsersChart dẫn tới khu vực biểu đồ #usersChartSection)
     */
    @Test
    @WithMockUser(username = "admin_uc25", roles = {"ADMIN"})
    void testStep3_ViewUsersChartSectionExists() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"usersChartSection\"")))
                .andExpect(content().string(containsString("id=\"newUserChartCard\"")))
                .andExpect(content().string(containsString("id=\"adminUsersChart\"")));
    }

    /**
     * Main Flow Bước 4:
     * 4. Chọn loại biểu đồ (đường, cột, tròn), chọn thời gian (giờ, ngày, tháng, năm)
     */
    @Test
    @WithMockUser(username = "admin_uc25", roles = {"ADMIN"})
    void testStep4_SelectChartTypeAndTimeRange() throws Exception {
        // Kiểm tra điều khiển loại biểu đồ (userLineBtn, userBarBtn, userPieBtn)
        // và điều khiển thời gian (userTimeHour, userTimeDay, userTimeMonth, userTimeYear)
        mockMvc.perform(get("/admin/dashboard")
                        .param("timeframe", "hour")
                        .param("userChartType", "bar"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("timeframe", "hour"))
                .andExpect(model().attribute("userChartType", "bar"))
                .andExpect(model().attributeExists("usersChart"))
                .andExpect(content().string(containsString("id=\"userLineBtn\"")))
                .andExpect(content().string(containsString("id=\"userBarBtn\"")))
                .andExpect(content().string(containsString("id=\"userPieBtn\"")))
                .andExpect(content().string(containsString("id=\"userTimeHour\"")))
                .andExpect(content().string(containsString("id=\"userTimeDay\"")))
                .andExpect(content().string(containsString("id=\"userTimeMonth\"")))
                .andExpect(content().string(containsString("id=\"userTimeYear\"")));

        // Kiểm tra theo tháng + tròn
        mockMvc.perform(get("/admin/dashboard")
                        .param("timeframe", "month")
                        .param("userChartType", "pie"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("timeframe", "month"))
                .andExpect(model().attribute("userChartType", "pie"))
                .andExpect(model().attributeExists("usersChart"));

        // Kiểm tra theo năm + đường
        mockMvc.perform(get("/admin/dashboard")
                        .param("timeframe", "year")
                        .param("userChartType", "line"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("timeframe", "year"))
                .andExpect(model().attribute("userChartType", "line"))
                .andExpect(model().attributeExists("usersChart"));
    }

    /**
     * Main Flow Bước 5 & Post-conditions:
     * 5. Hiển thị bên cạnh biểu đồ là Top người dùng điểm cao nhất hệ thống (topPointUsersCard)
     * - Danh sách lên đến 1000 người dùng
     */
    @Test
    @WithMockUser(username = "admin_uc25", roles = {"ADMIN"})
    void testStep5_DisplayTopPointUsersBesideChart() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("topPointUsers"))
                .andExpect(content().string(containsString("id=\"topPointUsersCard\"")))
                .andExpect(content().string(containsString("Khách Hàng Vip Nhất")))
                .andExpect(content().string(containsString("9800 pts")))
                .andExpect(content().string(containsString("Khách Hàng Vip Nhì")))
                .andExpect(content().string(containsString("7500 pts")));
    }

    /**
     * Pre-condition:
     * Đã đăng nhập vào hệ thống với quyền Admin
     * - Nếu chưa đăng nhập hoặc không có quyền Admin: không thể vào dashboard
     */
    @Test
    @WithMockUser(username = "customer_normal", roles = {"USER"})
    void testPreCondition_CustomerDeniedAccessToAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }
}
