package com.finalproject;

import com.finalproject.entity.Commission;
import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.repository.CommissionRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Kiểm thử toàn diện Use Case UC_035: Quản lý chính sách hoa hồng (System management – Commission)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminCommissionManagementUseCaseTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommissionRepository commissionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User customer;
    private Commission commission1;
    private Commission commission2;

    @BeforeEach
    void setUp() {
        String encodedPass = passwordEncoder.encode("secret123");

        admin = userRepository.save(User.builder()
                .username("admin_uc35")
                .email("admin_uc35@example.com")
                .password(encodedPass)
                .fullName("Quản Trị Viên UC35")
                .isActive(true).role(Role.ADMIN)
                .build());

        customer = userRepository.save(User.builder()
                .username("customer_uc35")
                .email("customer_uc35@example.com")
                .password(encodedPass)
                .fullName("Khách Hàng UC35")
                .isActive(true).role(Role.CUSTOMER)
                .build());

        commission1 = commissionRepository.save(Commission.builder()
                .name("Hoa hồng tiêu chuẩn UC35")
                .cost(new BigDecimal("10"))
                .description("Phí hoa hồng sàn 10% cho đơn hàng tiêu chuẩn")
                .isDeleted(false)
                .build());

        commission2 = commissionRepository.save(Commission.builder()
                .name("Phí hoa hồng cố định UC35")
                .cost(new BigDecimal("25000"))
                .description("Phí cố định 25.000đ mỗi đơn")
                .isDeleted(false)
                .build());
    }

    /**
     * Pre-condition: Chỉ người dùng có quyền Admin mới được phép truy cập
     */
    @Test
    @WithMockUser(username = "customer_uc35", roles = {"CUSTOMER"})
    void testPreCondition_CustomerDeniedAccessToAdminCommissions() throws Exception {
        mockMvc.perform(get("/admin/commissions"))
                .andExpect(status().isForbidden());
    }

    /**
     * Main Flow Bước 1, 2 & 3: Vào Dashboard, chọn Commission bên sidebar, hiển thị danh sách chính sách hoa hồng
     */
    @Test
    @WithMockUser(username = "admin_uc35", roles = {"ADMIN"})
    void testStep1To3_NavigateFromDashboardToCommissions() throws Exception {
        // Bước 1 & 2: Dashboard có link sidebar sang Commission
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"sidebarCommissionsLink\"")))
                .andExpect(content().string(containsString("href=\"/admin/commissions\"")));

        // Bước 3: Truy cập trang quản lý chính sách hoa hồng
        mockMvc.perform(get("/admin/commissions"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/commissions"))
                .andExpect(model().attributeExists("commissions"));
    }

    /**
     * Main Flow Bước 4: Hiển thị bảng danh sách chính sách hoa hồng và phân trang Next / Previous
     */
    @Test
    @WithMockUser(username = "admin_uc35", roles = {"ADMIN"})
    void testStep4_DisplayCommissionsTableAndPagination() throws Exception {
        // Tạo thêm các chính sách để phân trang
        for (int i = 1; i <= 10; i++) {
            commissionRepository.save(Commission.builder()
                    .name("Chính sách thử nghiệm UC35 " + i)
                    .cost(new BigDecimal(5 + i))
                    .description("Mô tả chính sách thử nghiệm " + i)
                    .isDeleted(false)
                    .build());
        }

        // Kiểm tra bảng danh sách chính sách
        mockMvc.perform(get("/admin/commissions?page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"commissionsTable\"")))
                .andExpect(content().string(containsString("id=\"commissionsPagination\"")))
                .andExpect(content().string(containsString("id=\"btnPrevPage\"")))
                .andExpect(content().string(containsString("id=\"btnNextPage\"")))
                .andExpect(content().string(containsString("id=\"pageBtn_1\"")))
                .andExpect(content().string(containsString("id=\"pageBtn_2\"")));

        // Chọn Next / page 1
        mockMvc.perform(get("/admin/commissions?page=1&size=5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"commissionsTable\"")))
                .andExpect(model().attribute("currentPage", 1));
    }

    /**
     * Main Flow Bước 5 - 11:
     * 5. Chọn nút New commission để thêm mới
     * 6. Hiển thị form thêm mới
     * 7. Nhập đầy đủ thông tin
     * 8. Bấm nút Submit
     * 9. Hiển thị thông báo yêu cầu xác nhận
     * 10. Bấm nút Confirm
     * 11. Thông báo thành công
     */
    @Test
    @WithMockUser(username = "admin_uc35", roles = {"ADMIN"})
    void testStep5To11_CreateNewCommissionFlow() throws Exception {
        // Bước 5 & 6: Truy cập form tạo mới từ nút New commission
        mockMvc.perform(get("/admin/commissions/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/commission-form"))
                .andExpect(content().string(containsString("id=\"commissionNameInput\"")))
                .andExpect(content().string(containsString("id=\"commissionCostInput\"")))
                .andExpect(content().string(containsString("id=\"commissionDescInput\"")))
                .andExpect(content().string(containsString("id=\"btnSubmitCommission\"")))
                .andExpect(content().string(containsString("id=\"confirmCommissionModal\"")))
                .andExpect(content().string(containsString("id=\"btnConfirmCommission\"")));

        // Bước 7 - 10: Gửi form tạo mới sau khi bấm Confirm
        mockMvc.perform(post("/admin/commissions/add")
                        .with(csrf())
                        .param("name", "Chính sách đối tác VIP 8%")
                        .param("cost", "8")
                        .param("description", "Dành cho đối tác cấp Platinum"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/commissions"))
                .andExpect(flash().attributeExists("successMessage"));

        // Bước 11: Kiểm tra thông báo thành công hiển thị trên giao diện danh sách
        mockMvc.perform(get("/admin/commissions")
                        .flashAttr("successMessage", "Thêm chính sách hoa hồng thành công!"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"successAlert\"")))
                .andExpect(content().string(containsString("Thêm chính sách hoa hồng thành công!")));

        // Kiểm tra cơ sở dữ liệu
        Commission created = commissionRepository.findAll().stream()
                .filter(c -> "Chính sách đối tác VIP 8%".equals(c.getName()))
                .findFirst()
                .orElse(null);
        assertThat(created).isNotNull();
        assertThat(created.getCost()).isEqualByComparingTo("8");
        assertThat(created.getDescription()).isEqualTo("Dành cho đối tác cấp Platinum");
        assertThat(created.getIsDeleted()).isFalse();
    }

    /**
     * Alternative 5a & 6a: Chọn Edit để chỉnh sửa chính sách hoa hồng
     */
    @Test
    @WithMockUser(username = "admin_uc35", roles = {"ADMIN"})
    void testAlternative5aAnd6a_EditCommissionFlow() throws Exception {
        // Kiểm tra nút Edit có trên bảng danh sách
        mockMvc.perform(get("/admin/commissions"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnEditCommission_" + commission1.getId() + "\"")));

        // 6a: Hiển thị form chỉnh sửa
        mockMvc.perform(get("/admin/commissions/edit/" + commission1.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/commission-form"))
                .andExpect(content().string(containsString("id=\"commissionNameInput\"")))
                .andExpect(content().string(containsString(commission1.getName())))
                .andExpect(content().string(containsString("id=\"btnSubmitCommission\"")))
                .andExpect(content().string(containsString("id=\"btnConfirmCommission\"")));

        // Submit form chỉnh sửa
        mockMvc.perform(post("/admin/commissions/update/" + commission1.getId())
                        .with(csrf())
                        .param("name", "Hoa hồng tiêu chuẩn đã sửa 12%")
                        .param("cost", "12")
                        .param("description", "Mô tả mới sau cập nhật"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/commissions"))
                .andExpect(flash().attributeExists("successMessage"));

        Commission updated = commissionRepository.findById(commission1.getId()).orElse(null);
        assertThat(updated).isNotNull();
        assertThat(updated.getName()).isEqualTo("Hoa hồng tiêu chuẩn đã sửa 12%");
        assertThat(updated.getCost()).isEqualByComparingTo("12");
        assertThat(updated.getDescription()).isEqualTo("Mô tả mới sau cập nhật");
    }

    /**
     * Alternative 5-8b & 5-8c: Chọn nút Delete để xóa và Restore để khôi phục chính sách hoa hồng
     */
    @Test
    @WithMockUser(username = "admin_uc35", roles = {"ADMIN"})
    void testAlternative5To8bc_DeleteAndRestoreCommissionFlow() throws Exception {
        // Kiểm tra nút Delete có trên bảng
        mockMvc.perform(get("/admin/commissions"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnDeleteCommission_" + commission1.getId() + "\"")));

        // 5-8b: Xóa chính sách hoa hồng (soft delete)
        mockMvc.perform(post("/admin/commissions/delete/" + commission1.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/commissions"))
                .andExpect(flash().attributeExists("successMessage"));

        Commission deleted = commissionRepository.findById(commission1.getId()).orElse(null);
        assertThat(deleted).isNotNull();
        assertThat(deleted.getIsDeleted()).isTrue();

        // Kiểm tra nút Restore xuất hiện khi đã bị xóa
        mockMvc.perform(get("/admin/commissions"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnRestoreCommission_" + commission1.getId() + "\"")));

        // 5-8c: Khôi phục chính sách hoa hồng
        mockMvc.perform(post("/admin/commissions/restore/" + commission1.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/commissions"))
                .andExpect(flash().attributeExists("successMessage"));

        Commission restored = commissionRepository.findById(commission1.getId()).orElse(null);
        assertThat(restored).isNotNull();
        assertThat(restored.getIsDeleted()).isFalse();
    }

    /**
     * Alternative 10d & 11d: Bấm Cancel trên form để hủy thao tác
     */
    @Test
    @WithMockUser(username = "admin_uc35", roles = {"ADMIN"})
    void testAlternative10dAnd11d_CancelAction() throws Exception {
        mockMvc.perform(get("/admin/commissions/create"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnCancelCommission\"")))
                .andExpect(content().string(containsString("href=\"/admin/commissions\"")));
    }

    /**
     * Post-conditions: Hiển thị thông báo lỗi khi có lỗi tải dữ liệu
     */
    @Test
    @WithMockUser(username = "admin_uc35", roles = {"ADMIN"})
    void testPostCondition_ErrorAlertDisplay() throws Exception {
        mockMvc.perform(get("/admin/commissions")
                        .flashAttr("errorMessage", "Không thể tải danh sách chính sách hoa hồng!"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"dataLoadAlert\"")))
                .andExpect(content().string(containsString("Không thể tải danh sách chính sách hoa hồng!")));
    }
}
