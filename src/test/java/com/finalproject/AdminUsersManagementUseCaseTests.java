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
 * Kiểm thử Use Case UC_027: Quản lý người dùng trong hệ thống (Users management)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminUsersManagementUseCaseTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User customer1;
    private User customer2;
    private User customer3;
    private User customer4;

    @BeforeEach
    void setUp() {
        String encodedPass = passwordEncoder.encode("secret123");

        admin = userRepository.save(User.builder()
                .username("admin_uc27")
                .email("admin_uc27@example.com")
                .password(encodedPass)
                .fullName("Quản Trị Viên UC27")
                .role(Role.ADMIN)
                .isActive(true)
                .createdDate(LocalDate.now())
                .build());

        customer1 = userRepository.save(User.builder()
                .username("user1_uc27")
                .email("user1_uc27@example.com")
                .password(encodedPass)
                .fullName("Người Dùng Một")
                .role(Role.CUSTOMER)
                .isActive(true)
                .createdDate(LocalDate.now())
                .build());

        customer2 = userRepository.save(User.builder()
                .username("user2_uc27")
                .email("user2_uc27@example.com")
                .password(encodedPass)
                .fullName("Người Dùng Hai")
                .role(Role.CUSTOMER)
                .isActive(true)
                .createdDate(LocalDate.now())
                .build());

        customer3 = userRepository.save(User.builder()
                .username("user3_uc27")
                .email("user3_uc27@example.com")
                .password(encodedPass)
                .fullName("Người Dùng Ba")
                .role(Role.CUSTOMER)
                .isActive(true)
                .createdDate(LocalDate.now())
                .build());

        customer4 = userRepository.save(User.builder()
                .username("user4_uc27")
                .email("user4_uc27@example.com")
                .password(encodedPass)
                .fullName("Người Dùng Bốn")
                .role(Role.CUSTOMER)
                .isActive(true)
                .createdDate(LocalDate.now())
                .build());
    }

    /**
     * Pre-condition: Chỉ người dùng có vai trò ADMIN mới được truy cập vào trang Quản lý người dùng.
     * Người dùng vai trò CUSTOMER sẽ bị từ chối truy cập (403 Forbidden).
     */
    @Test
    @WithMockUser(username = "customer_denied_uc27", roles = {"CUSTOMER"})
    void testPreCondition_CustomerDeniedAccessToAdminUsers() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    /**
     * Main Flow Bước 1 & 2:
     * 1. Vào trang Dashboard của Admin (/admin/dashboard)
     * 2. Chọn Users bên thanh sidebar (id="sidebarUsersLink" dẫn tới /admin/users)
     */
    @Test
    @WithMockUser(username = "admin_uc27", roles = {"ADMIN"})
    void testStep1And2_NavigateFromDashboardToUsers() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(content().string(containsString("id=\"sidebarUsersLink\"")))
                .andExpect(content().string(containsString("href=\"/admin/users\"")));
    }

    /**
     * Main Flow Bước 3:
     * 3. Hiển thị bảng danh sách tất cả người dùng trong hệ thống (id="usersTable")
     * Post-condition (thành công): Hiển thị bảng danh sách tất cả người dùng
     */
    @Test
    @WithMockUser(username = "admin_uc27", roles = {"ADMIN"})
    void testStep3_DisplayAllUsersTable() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attributeExists("userPage"))
                .andExpect(model().attributeExists("totalPages"))
                .andExpect(model().attributeExists("totalElements"))
                .andExpect(model().attributeExists("currentPage"))
                .andExpect(content().string(containsString("id=\"usersTable\"")))
                .andExpect(content().string(containsString("Quản Lý Người Dùng & Vai Trò")))
                .andExpect(content().string(containsString("user1_uc27")))
                .andExpect(content().string(containsString("user2_uc27")));
    }

    /**
     * Main Flow Bước 4:
     * 4. Chọn Next (hoặc Previous, hoặc trang bất kỳ) để xem trang tiếp theo (hoặc trước đó, hoặc trang tương ứng)
     */
    @Test
    @WithMockUser(username = "admin_uc27", roles = {"ADMIN"})
    void testStep4_PaginationNextPreviousAndDirectPage() throws Exception {
        // Truy cập trang đầu tiên với kích thước trang là 2 (để tạo nhiều trang)
        mockMvc.perform(get("/admin/users")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(content().string(containsString("id=\"usersPagination\"")))
                .andExpect(content().string(containsString("id=\"btnNextPage\"")))
                .andExpect(content().string(containsString("id=\"btnPrevPage\"")))
                .andExpect(content().string(containsString("id=\"pageBtn_1\"")))
                .andExpect(content().string(containsString("id=\"pageBtn_2\"")));

        // Bấm Next (chọn trang tiếp theo: page=1)
        mockMvc.perform(get("/admin/users")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attribute("currentPage", 1))
                .andExpect(content().string(containsString("id=\"usersPagination\"")));
    }

    /**
     * Post-condition (thất bại): Nếu thất bại hoặc có lỗi tải dữ liệu, thông báo lỗi được hiển thị.
     */
    @Test
    @WithMockUser(username = "admin_uc27", roles = {"ADMIN"})
    void testPostCondition_ErrorNotificationWhenDataLoadFails() throws Exception {
        mockMvc.perform(get("/admin/users").flashAttr("dataLoadError", true).flashAttr("errorMessage", "Không thể tải danh sách người dùng. Vui lòng thử lại!"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"dataLoadAlert\"")))
                .andExpect(content().string(containsString("Không thể tải danh sách người dùng. Vui lòng thử lại!")));
    }
}
