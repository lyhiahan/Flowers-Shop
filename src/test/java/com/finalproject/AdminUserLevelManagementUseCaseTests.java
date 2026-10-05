package com.finalproject;

import com.finalproject.entity.User;
import com.finalproject.entity.UserLevel;
import com.finalproject.enums.Role;
import com.finalproject.repository.UserLevelRepository;
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
 * Kiểm thử toàn diện Use Case UC_032: Quản lý cấp độ người dùng (System management – User level)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@SuppressWarnings("unused")
class AdminUserLevelManagementUseCaseTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserLevelRepository userLevelRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User customer;
    private UserLevel level1;
    private UserLevel level2;

    @BeforeEach
    void setUp() {
        String encodedPass = passwordEncoder.encode("secret123");

        admin = userRepository.save(User.builder()
                .username("admin_uc32")
                .email("admin_uc32@example.com")
                .password(encodedPass)
                .fullName("Quản Trị Viên UC32")
                .isActive(true).role(Role.ADMIN)
                .build());

        customer = userRepository.save(User.builder()
                .username("customer_uc32")
                .email("customer_uc32@example.com")
                .password(encodedPass)
                .fullName("Khách Hàng UC32")
                .isActive(true).role(Role.USER)
                .build());

        level1 = userLevelRepository.save(UserLevel.builder()
                .name("Đồng Tiêu Chuẩn UC32")
                .minPoint(0)
                .discount(BigDecimal.ZERO)
                .isDeleted(false)
                .build());

        level2 = userLevelRepository.save(UserLevel.builder()
                .name("Vàng VIP UC32")
                .minPoint(500)
                .discount(new BigDecimal("5.0"))
                .isDeleted(false)
                .build());
    }

    /**
     * Pre-condition: Chỉ người dùng có quyền Admin mới được phép truy cập
     */
    @Test
    @WithMockUser(username = "customer_uc32", roles = {"USER"})
    void testPreCondition_CustomerDeniedAccessToAdminUserLevels() throws Exception {
        mockMvc.perform(get("/admin/user-levels"))
                .andExpect(status().isForbidden());
    }

    /**
     * Main Flow Bước 1, 2 & 3: Vào Dashboard, chọn Level bên sidebar, chọn User level
     */
    @Test
    @WithMockUser(username = "admin_uc32", roles = {"ADMIN"})
    void testStep1To3_NavigateFromDashboardToUserLevels() throws Exception {
        // Bước 1 & 2: Dashboard có link sidebar sang User level
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"sidebarUserLevelsLink\"")))
                .andExpect(content().string(containsString("href=\"/admin/user-levels\"")));

        // Bước 3: Truy cập trang quản lý user levels
        mockMvc.perform(get("/admin/user-levels"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-levels"))
                .andExpect(model().attributeExists("levels"));
    }

    /**
     * Main Flow Bước 4 & 5: Hiển thị bảng danh sách các user level và phân trang Next / Previous
     */
    @Test
    @WithMockUser(username = "admin_uc32", roles = {"ADMIN"})
    void testStep4And5_DisplayUserLevelsTableAndPagination() throws Exception {
        // Tạo thêm các level để phân trang
        for (int i = 1; i <= 10; i++) {
            userLevelRepository.save(UserLevel.builder()
                    .name("Hạng Thử Nghiệm UC32 " + i)
                    .minPoint(1000 + i * 100)
                    .discount(new BigDecimal(i))
                    .isDeleted(false)
                    .build());
        }

        // Bước 4: Kiểm tra bảng danh sách user level
        mockMvc.perform(get("/admin/user-levels?page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"userLevelsTable\"")))
                .andExpect(content().string(containsString("id=\"userLevelsPagination\"")))
                .andExpect(content().string(containsString("id=\"btnPrevPage\"")))
                .andExpect(content().string(containsString("id=\"btnNextPage\"")))
                .andExpect(content().string(containsString("id=\"pageBtn_1\"")))
                .andExpect(content().string(containsString("id=\"pageBtn_2\"")));

        // Bước 5: Chọn Next / page 1
        mockMvc.perform(get("/admin/user-levels?page=1&size=5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"userLevelsTable\"")))
                .andExpect(model().attribute("currentPage", 1));
    }

    /**
     * Main Flow Bước 6 - 12:
     * 6. Chọn nút New level để thêm level mới
     * 7. Hiển thị form thêm level mới
     * 8. Nhập đầy đủ thông tin
     * 9. Bấm Submit
     * 10. Hiển thị thông báo yêu cầu xác nhận
     * 11. Bấm Confirm
     * 12. Thông báo thành công
     */
    @Test
    @WithMockUser(username = "admin_uc32", roles = {"ADMIN"})
    void testStep6To12_CreateNewUserLevelWithConfirmModal() throws Exception {
        // Bước 6: Trang user-levels có nút New level
        mockMvc.perform(get("/admin/user-levels"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnNewUserLevel\"")))
                .andExpect(content().string(containsString("href=\"/admin/user-levels/create\"")));

        // Bước 7 & 8: Chuyển đến form tạo level mới
        mockMvc.perform(get("/admin/user-levels/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-level-form"))
                .andExpect(content().string(containsString("id=\"userLevelNameInput\"")))
                .andExpect(content().string(containsString("id=\"userLevelMinPointInput\"")))
                .andExpect(content().string(containsString("id=\"userLevelDiscountInput\"")))
                // Bước 9: Nút Submit
                .andExpect(content().string(containsString("id=\"btnSubmitUserLevel\"")))
                // Bước 10: Modal xác nhận
                .andExpect(content().string(containsString("id=\"confirmUserLevelModal\"")))
                // Bước 11: Nút Confirm
                .andExpect(content().string(containsString("id=\"btnConfirmUserLevel\"")))
                // Alternative 11d: Nút Cancel
                .andExpect(content().string(containsString("id=\"btnCancelUserLevel\"")));

        // Bước 11 & 12: Submit form tạo level mới -> Thông báo thành công
        mockMvc.perform(post("/admin/user-levels/add")
                        .with(csrf())
                        .param("name", "Bạch Kim Hoàng Gia UC32")
                        .param("minPoint", "2000")
                        .param("discount", "12.5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/user-levels"))
                .andExpect(flash().attributeExists("successMessage"));

        // Kiểm tra user level được lưu trong cơ sở dữ liệu
        UserLevel saved = userLevelRepository.findAll().stream()
                .filter(l -> "Bạch Kim Hoàng Gia UC32".equals(l.getName()))
                .findFirst()
                .orElse(null);
        assertThat(saved).isNotNull();
        assertThat(saved.getMinPoint()).isEqualTo(2000);
        assertThat(saved.getDiscount()).isEqualByComparingTo(new BigDecimal("12.5"));
        assertThat(saved.getIsDeleted()).isFalse();

        // Kiểm tra hiển thị thông báo thành công trên trang user levels
        mockMvc.perform(get("/admin/user-levels")
                        .flashAttr("successMessage", "Thêm mới cấp bậc khách hàng [Bạch Kim Hoàng Gia UC32] thành công!"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"successAlert\"")))
                .andExpect(content().string(containsString("Bạch Kim Hoàng Gia UC32")));
    }

    /**
     * Alternative Flow 6a & 7a: Chọn nút Edit tại level muốn chỉnh sửa -> Hiển thị form chỉnh sửa
     */
    @Test
    @WithMockUser(username = "admin_uc32", roles = {"ADMIN"})
    void testAlternative6aAnd7a_EditUserLevel() throws Exception {
        // 6a: Trang user levels có nút Sửa với id btnEditUserLevel_{id}
        mockMvc.perform(get("/admin/user-levels"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnEditUserLevel_" + level2.getId() + "\"")))
                .andExpect(content().string(containsString("href=\"/admin/user-levels/edit/" + level2.getId() + "\"")));

        // 7a: Truy cập form chỉnh sửa user level
        mockMvc.perform(get("/admin/user-levels/edit/" + level2.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-level-form"))
                .andExpect(model().attribute("isEdit", true))
                .andExpect(content().string(containsString(level2.getName())))
                .andExpect(content().string(containsString("id=\"btnSubmitUserLevel\"")))
                .andExpect(content().string(containsString("id=\"confirmUserLevelModal\"")))
                .andExpect(content().string(containsString("id=\"btnConfirmUserLevel\"")));

        // Thực hiện cập nhật
        mockMvc.perform(post("/admin/user-levels/update/" + level2.getId())
                        .with(csrf())
                        .param("name", "Vàng VIP Cao Cấp UC32")
                        .param("minPoint", "600")
                        .param("discount", "7.0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/user-levels"))
                .andExpect(flash().attributeExists("successMessage"));

        UserLevel updated = userLevelRepository.findById(level2.getId()).orElse(null);
        assertThat(updated).isNotNull();
        assertThat(updated.getName()).isEqualTo("Vàng VIP Cao Cấp UC32");
        assertThat(updated.getMinPoint()).isEqualTo(600);
        assertThat(updated.getDiscount()).isEqualByComparingTo(new BigDecimal("7.0"));
    }

    /**
     * Alternative Flow 6-10b & 6-10c: Xóa level (Delete) và Khôi phục level (Restore)
     */
    @Test
    @WithMockUser(username = "admin_uc32", roles = {"ADMIN"})
    void testAlternative6To10bAnd6To10c_DeleteAndRestoreUserLevel() throws Exception {
        // Kiểm tra nút xóa xuất hiện khi level chưa xóa
        mockMvc.perform(get("/admin/user-levels"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnDeleteUserLevel_" + level1.getId() + "\"")));

        // 6-10b: Thực hiện XÓA level (Soft delete)
        mockMvc.perform(post("/admin/user-levels/delete/" + level1.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/user-levels"))
                .andExpect(flash().attributeExists("successMessage"));

        UserLevel deleted = userLevelRepository.findById(level1.getId()).orElse(null);
        assertThat(deleted).isNotNull();
        assertThat(deleted.getIsDeleted()).isTrue();

        // Sau khi xóa, trang danh sách hiển thị nhãn ĐÃ XÓA và nút Khôi phục
        mockMvc.perform(get("/admin/user-levels"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ĐÃ XÓA")))
                .andExpect(content().string(containsString("id=\"btnRestoreUserLevel_" + level1.getId() + "\"")));

        // 6-10c: Thực hiện KHÔI PHỤC level
        mockMvc.perform(post("/admin/user-levels/restore/" + level1.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/user-levels"))
                .andExpect(flash().attributeExists("successMessage"));

        UserLevel restored = userLevelRepository.findById(level1.getId()).orElse(null);
        assertThat(restored).isNotNull();
        assertThat(restored.getIsDeleted()).isFalse();

        // Sau khi khôi phục, trang danh sách trở lại trạng thái ĐANG ÁP DỤNG và có nút Xóa
        mockMvc.perform(get("/admin/user-levels"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ĐANG ÁP DỤNG")))
                .andExpect(content().string(containsString("id=\"btnDeleteUserLevel_" + level1.getId() + "\"")));
    }

    /**
     * Alternative Flow 11d & 12d: Bấm nút Cancel -> Hủy thao tác
     */
    @Test
    @WithMockUser(username = "admin_uc32", roles = {"ADMIN"})
    void testAlternative11dAnd12d_CancelActions() throws Exception {
        mockMvc.perform(get("/admin/user-levels/create"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnCancelUserLevel\"")))
                .andExpect(content().string(containsString("data-bs-dismiss=\"modal\"")));
    }

    /**
     * Post-condition: Nếu thất bại, thông báo lỗi (dataLoadAlert)
     */
    @Test
    @WithMockUser(username = "admin_uc32", roles = {"ADMIN"})
    void testPostCondition_ErrorNotificationWhenDataLoadFails() throws Exception {
        mockMvc.perform(get("/admin/user-levels")
                        .flashAttr("errorMessage", "Không thể tải danh sách cấp bậc khách hàng!")
                        .flashAttr("dataLoadError", true))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"dataLoadAlert\"")))
                .andExpect(content().string(containsString("Không thể tải danh sách cấp bậc khách hàng!")));
    }
}
