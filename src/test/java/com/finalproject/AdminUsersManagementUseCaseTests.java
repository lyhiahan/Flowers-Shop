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
@SuppressWarnings("unused")
class AdminUsersManagementUseCaseTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.finalproject.service.CustomerAccountClosureService accountClosure;

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
                .role(Role.USER)
                .isActive(true)
                .createdDate(LocalDate.now())
                .build());

        customer2 = userRepository.save(User.builder()
                .username("user2_uc27")
                .email("user2_uc27@example.com")
                .password(encodedPass)
                .fullName("Người Dùng Hai")
                .role(Role.USER)
                .isActive(true)
                .createdDate(LocalDate.now())
                .build());

        customer3 = userRepository.save(User.builder()
                .username("user3_uc27")
                .email("user3_uc27@example.com")
                .password(encodedPass)
                .fullName("Người Dùng Ba")
                .role(Role.USER)
                .isActive(true)
                .createdDate(LocalDate.now())
                .build());

        customer4 = userRepository.save(User.builder()
                .username("user4_uc27")
                .email("user4_uc27@example.com")
                .password(encodedPass)
                .fullName("Người Dùng Bốn")
                .role(Role.USER)
                .isActive(true)
                .createdDate(LocalDate.now())
                .build());
    }

    /**
     * Pre-condition: Chỉ người dùng có vai trò ADMIN mới được truy cập vào trang Quản lý người dùng.
     * Người dùng vai trò USER sẽ bị từ chối truy cập (403 Forbidden).
     */
    @Test
    @WithMockUser(username = "customer_denied_uc27", roles = {"USER"})
    void testPreCondition_CustomerDeniedAccessToAdminUsers() throws Exception {
        // Use a real active account so this checks authorization, not session revocation.
        userRepository.saveAndFlush(User.builder().username("customer_denied_uc27").email("customer_denied_uc27@fixture.test")
                .password("unused-fixture-password").fullName("Access test").role(Role.USER).isActive(true).build());
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
                .andExpect(content().string(containsString("Quản Lý Người Dùng &amp; Vai Trò")))
                .andExpect(content().string(containsString("user1_uc27")))
                .andExpect(content().string(containsString("user2_uc27")));
    }

    @Test
    @WithMockUser(username = "admin_uc27", roles = {"ADMIN"})
    void combinedRoleAndKeywordAndPhoneSearchMustAllApply() throws Exception {
        customer1.setPhone("0901234567"); userRepository.saveAndFlush(customer1);
        mockMvc.perform(get("/admin/users").param("role", "ADMIN").param("keyword", "user1_uc27"))
                .andExpect(status().isOk()).andExpect(model().attribute("totalElements", 0L));
        mockMvc.perform(get("/admin/users").param("role", "USER").param("keyword", "0901234567"))
                .andExpect(status().isOk()).andExpect(model().attribute("totalElements", 1L))
                .andExpect(content().string(containsString("@user1_uc27")));
    }

    @Test
    @WithMockUser(username = "admin_uc27", roles = {"ADMIN"})
    void renamedAdminCannotBeDisabledThroughDirectRequest() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/admin/users/toggle-active/{id}", admin.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage", containsString("Không thể khóa")));
        org.assertj.core.api.Assertions.assertThat(userRepository.findById(admin.getId()).orElseThrow().getIsActive()).isTrue();
    }

    @Test
    @WithMockUser(username = "admin_uc27", roles = {"ADMIN"})
    void accountClosureServiceAlsoProtectsRenamedAdmin() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> accountClosure.disable(admin.getId(), false))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Quản trị viên");
        org.assertj.core.api.Assertions.assertThat(admin.getIsActive()).isTrue();
    }

    @Test
    @WithMockUser(username = "admin_uc27", roles = {"ADMIN"})
    void outOfRangePageFallsBackToLastAvailablePage() throws Exception {
        mockMvc.perform(get("/admin/users").param("page", "999").param("size", "2"))
                .andExpect(status().isOk()).andExpect(model().attribute("currentPage", 2))
                .andExpect(model().attribute("users", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    @WithMockUser(username = "admin_uc27", roles = {"ADMIN"})
    void defaultPageRendersAllFifteenUsersAndPageSizeChoices() throws Exception {
        for (int i = 0; i < 10; i++) {
            userRepository.save(User.builder().username("extra_uc27_" + i)
                    .email("extra_uc27_" + i + "@example.test").password(admin.getPassword())
                    .fullName("Khách kiểm thử " + i).role(Role.USER).isActive(true).build());
        }
        var allUsers = mockMvc.perform(get("/admin/users").param("role", "ALL").param("keyword", ""))
                .andExpect(status().isOk())
                .andExpect(model().attribute("totalElements", 15L))
                .andExpect(model().attribute("pageSize", 20))
                .andExpect(model().attribute("users", org.hamcrest.Matchers.hasSize(15)))
                .andExpect(content().string(containsString("id=\"usersPageSize\"")))
                .andExpect(content().string(containsString("id=\"usersPagination\"")))
                .andReturn().getResponse().getContentAsString();
        for (int n : new int[] {5, 10, 20, 50}) {
            org.assertj.core.api.Assertions.assertThat(allUsers).contains(n + " / trang</option>");
        }
        for (User user : userRepository.findAll()) {
            org.assertj.core.api.Assertions.assertThat(allUsers).contains("@" + user.getUsername());
        }
        mockMvc.perform(get("/admin/users").param("role", "ALL").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("totalPages", 3))
                .andExpect(model().attribute("users", org.hamcrest.Matchers.hasSize(5)))
                .andExpect(content().string(containsString("role=ALL&amp;page=1&amp;size=5")))
                .andExpect(content().string(containsString("Trang sau")));
        mockMvc.perform(get("/admin/users").param("size", "50").param("role", "USER"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("users", org.hamcrest.Matchers.hasSize(14)));
    }

    @Test
    @WithMockUser(username = "admin_uc27", roles = {"ADMIN"})
    void pageSizeIsBoundedAndEmptySearchStillRendersPagination() throws Exception {
        mockMvc.perform(get("/admin/users").param("size", "1000000"))
                .andExpect(status().isOk()).andExpect(model().attribute("pageSize", 50));
        mockMvc.perform(get("/admin/users").param("size", "0"))
                .andExpect(status().isOk()).andExpect(model().attribute("pageSize", 1));
        mockMvc.perform(get("/admin/users").param("keyword", "no_matching_user_uc27"))
                .andExpect(status().isOk()).andExpect(model().attribute("totalElements", 0L))
                .andExpect(content().string(containsString("Không tìm thấy người dùng")))
                .andExpect(content().string(containsString("id=\"usersPagination\"")));
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
