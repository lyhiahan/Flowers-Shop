package com.finalproject;

import com.finalproject.entity.StoreLevel;
import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.repository.StoreLevelRepository;
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
 * Kiểm thử toàn diện Use Case UC_033: Quản lý cấp độ cửa hàng (System management – Store level)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminStoreLevelManagementUseCaseTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoreLevelRepository storeLevelRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User customer;
    private StoreLevel level1;
    private StoreLevel level2;

    @BeforeEach
    void setUp() {
        String encodedPass = passwordEncoder.encode("secret123");

        admin = userRepository.save(User.builder()
                .username("admin_uc33")
                .email("admin_uc33@example.com")
                .password(encodedPass)
                .fullName("Quản Trị Viên UC33")
                .role(Role.ADMIN)
                .build());

        customer = userRepository.save(User.builder()
                .username("customer_uc33")
                .email("customer_uc33@example.com")
                .password(encodedPass)
                .fullName("Khách Hàng UC33")
                .role(Role.CUSTOMER)
                .build());

        level1 = storeLevelRepository.save(StoreLevel.builder()
                .name("Tiêu Chuẩn UC33")
                .minPoint(0)
                .discount(BigDecimal.ZERO)
                .isDeleted(false)
                .build());

        level2 = storeLevelRepository.save(StoreLevel.builder()
                .name("Cửa Hàng Uy Tín UC33")
                .minPoint(10000000)
                .discount(new BigDecimal("5.0"))
                .isDeleted(false)
                .build());
    }

    /**
     * Pre-condition: Chỉ người dùng có quyền Admin mới được phép truy cập
     */
    @Test
    @WithMockUser(username = "customer_uc33", roles = {"CUSTOMER"})
    void testPreCondition_CustomerDeniedAccessToAdminStoreLevels() throws Exception {
        mockMvc.perform(get("/admin/store-levels"))
                .andExpect(status().isForbidden());
    }

    /**
     * Main Flow Bước 1, 2 & 3: Vào Dashboard, chọn Level bên sidebar, chọn Store level
     */
    @Test
    @WithMockUser(username = "admin_uc33", roles = {"ADMIN"})
    void testStep1To3_NavigateFromDashboardToStoreLevels() throws Exception {
        // Bước 1 & 2: Dashboard có link sidebar sang Store level
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"sidebarStoreLevelsLink\"")))
                .andExpect(content().string(containsString("href=\"/admin/store-levels\"")));

        // Bước 3: Truy cập trang quản lý store levels
        mockMvc.perform(get("/admin/store-levels"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/store-levels"))
                .andExpect(model().attributeExists("levels"));
    }

    /**
     * Main Flow Bước 4 & 5: Hiển thị bảng danh sách các store level và phân trang Next / Previous
     */
    @Test
    @WithMockUser(username = "admin_uc33", roles = {"ADMIN"})
    void testStep4And5_DisplayStoreLevelsTableAndPagination() throws Exception {
        // Tạo thêm các level để phân trang
        for (int i = 1; i <= 10; i++) {
            storeLevelRepository.save(StoreLevel.builder()
                    .name("Hạng Chi Nhánh UC33 " + i)
                    .minPoint(20000000 + i * 1000000)
                    .discount(new BigDecimal(i))
                    .isDeleted(false)
                    .build());
        }

        // Bước 4: Kiểm tra bảng danh sách store level
        mockMvc.perform(get("/admin/store-levels?page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"storeLevelsTable\"")))
                .andExpect(content().string(containsString("id=\"storeLevelsPagination\"")))
                .andExpect(content().string(containsString("id=\"btnPrevPage\"")))
                .andExpect(content().string(containsString("id=\"btnNextPage\"")))
                .andExpect(content().string(containsString("id=\"pageBtn_1\"")))
                .andExpect(content().string(containsString("id=\"pageBtn_2\"")));

        // Bước 5: Chọn Next / page 1
        mockMvc.perform(get("/admin/store-levels?page=1&size=5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"storeLevelsTable\"")))
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
    @WithMockUser(username = "admin_uc33", roles = {"ADMIN"})
    void testStep6To12_CreateNewStoreLevelWithConfirmModal() throws Exception {
        // Bước 6: Trang store-levels có nút New level
        mockMvc.perform(get("/admin/store-levels"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnNewStoreLevel\"")))
                .andExpect(content().string(containsString("href=\"/admin/store-levels/create\"")));

        // Bước 7 & 8: Chuyển đến form tạo level mới
        mockMvc.perform(get("/admin/store-levels/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/store-level-form"))
                .andExpect(content().string(containsString("id=\"storeLevelNameInput\"")))
                .andExpect(content().string(containsString("id=\"storeLevelMinPointInput\"")))
                .andExpect(content().string(containsString("id=\"storeLevelDiscountInput\"")))
                // Bước 9: Nút Submit
                .andExpect(content().string(containsString("id=\"btnSubmitStoreLevel\"")))
                // Bước 10: Modal xác nhận
                .andExpect(content().string(containsString("id=\"confirmStoreLevelModal\"")))
                // Bước 11: Nút Confirm
                .andExpect(content().string(containsString("id=\"btnConfirmStoreLevel\"")))
                // Alternative 11d: Nút Cancel
                .andExpect(content().string(containsString("id=\"btnCancelStoreLevel\"")));

        // Bước 11 & 12: Submit form tạo store level mới -> Thông báo thành công
        mockMvc.perform(post("/admin/store-levels/add")
                        .with(csrf())
                        .param("name", "Cửa Hàng Kim Cương UC33")
                        .param("minPoint", "50000000")
                        .param("discount", "15.0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/store-levels"))
                .andExpect(flash().attributeExists("successMessage"));

        // Kiểm tra store level được lưu trong cơ sở dữ liệu
        StoreLevel saved = storeLevelRepository.findAll().stream()
                .filter(l -> "Cửa Hàng Kim Cương UC33".equals(l.getName()))
                .findFirst()
                .orElse(null);
        assertThat(saved).isNotNull();
        assertThat(saved.getMinPoint()).isEqualTo(50000000);
        assertThat(saved.getDiscount()).isEqualByComparingTo(new BigDecimal("15.0"));
        assertThat(saved.getIsDeleted()).isFalse();

        // Kiểm tra hiển thị thông báo thành công trên trang store levels
        mockMvc.perform(get("/admin/store-levels")
                        .flashAttr("successMessage", "Thêm mới cấp bậc cửa hàng [Cửa Hàng Kim Cương UC33] thành công!"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"successAlert\"")))
                .andExpect(content().string(containsString("Cửa Hàng Kim Cương UC33")));
    }

    /**
     * Alternative Flow 6a & 7a: Chọn nút Edit tại level muốn chỉnh sửa -> Hiển thị form chỉnh sửa
     */
    @Test
    @WithMockUser(username = "admin_uc33", roles = {"ADMIN"})
    void testAlternative6aAnd7a_EditStoreLevel() throws Exception {
        // 6a: Trang store levels có nút Sửa với id btnEditStoreLevel_{id}
        mockMvc.perform(get("/admin/store-levels"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnEditStoreLevel_" + level2.getId() + "\"")))
                .andExpect(content().string(containsString("href=\"/admin/store-levels/edit/" + level2.getId() + "\"")));

        // 7a: Truy cập form chỉnh sửa store level
        mockMvc.perform(get("/admin/store-levels/edit/" + level2.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/store-level-form"))
                .andExpect(model().attribute("isEdit", true))
                .andExpect(content().string(containsString(level2.getName())))
                .andExpect(content().string(containsString("id=\"btnSubmitStoreLevel\"")))
                .andExpect(content().string(containsString("id=\"confirmStoreLevelModal\"")))
                .andExpect(content().string(containsString("id=\"btnConfirmStoreLevel\"")));

        // Thực hiện cập nhật
        mockMvc.perform(post("/admin/store-levels/update/" + level2.getId())
                        .with(csrf())
                        .param("name", "Cửa Hàng Uy Tín VIP UC33")
                        .param("minPoint", "12000000")
                        .param("discount", "7.5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/store-levels"))
                .andExpect(flash().attributeExists("successMessage"));

        StoreLevel updated = storeLevelRepository.findById(level2.getId()).orElse(null);
        assertThat(updated).isNotNull();
        assertThat(updated.getName()).isEqualTo("Cửa Hàng Uy Tín VIP UC33");
        assertThat(updated.getMinPoint()).isEqualTo(12000000);
        assertThat(updated.getDiscount()).isEqualByComparingTo(new BigDecimal("7.5"));
    }

    /**
     * Alternative Flow 6-10b & 6-10c: Xóa level (Delete) và Khôi phục level (Restore)
     */
    @Test
    @WithMockUser(username = "admin_uc33", roles = {"ADMIN"})
    void testAlternative6To10bAnd6To10c_DeleteAndRestoreStoreLevel() throws Exception {
        // Kiểm tra nút xóa xuất hiện khi level chưa xóa
        mockMvc.perform(get("/admin/store-levels"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnDeleteStoreLevel_" + level1.getId() + "\"")));

        // 6-10b: Thực hiện XÓA level (Soft delete)
        mockMvc.perform(post("/admin/store-levels/delete/" + level1.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/store-levels"))
                .andExpect(flash().attributeExists("successMessage"));

        StoreLevel deleted = storeLevelRepository.findById(level1.getId()).orElse(null);
        assertThat(deleted).isNotNull();
        assertThat(deleted.getIsDeleted()).isTrue();

        // Sau khi xóa, trang danh sách hiển thị nhãn Đã xóa và nút Khôi phục
        mockMvc.perform(get("/admin/store-levels"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Đã xóa")))
                .andExpect(content().string(containsString("id=\"btnRestoreStoreLevel_" + level1.getId() + "\"")));

        // 6-10c: Thực hiện KHÔI PHỤC level
        mockMvc.perform(post("/admin/store-levels/restore/" + level1.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/store-levels"))
                .andExpect(flash().attributeExists("successMessage"));

        StoreLevel restored = storeLevelRepository.findById(level1.getId()).orElse(null);
        assertThat(restored).isNotNull();
        assertThat(restored.getIsDeleted()).isFalse();

        // Sau khi khôi phục, trang danh sách trở lại trạng thái Đang áp dụng và có nút Xóa
        mockMvc.perform(get("/admin/store-levels"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Đang áp dụng")))
                .andExpect(content().string(containsString("id=\"btnDeleteStoreLevel_" + level1.getId() + "\"")));
    }

    /**
     * Alternative Flow 11d & 12d: Bấm nút Cancel -> Hủy thao tác
     */
    @Test
    @WithMockUser(username = "admin_uc33", roles = {"ADMIN"})
    void testAlternative11dAnd12d_CancelActions() throws Exception {
        mockMvc.perform(get("/admin/store-levels/create"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnCancelStoreLevel\"")))
                .andExpect(content().string(containsString("data-bs-dismiss=\"modal\"")));
    }

    /**
     * Post-condition: Nếu thất bại, thông báo lỗi (dataLoadAlert)
     */
    @Test
    @WithMockUser(username = "admin_uc33", roles = {"ADMIN"})
    void testPostCondition_ErrorNotificationWhenDataLoadFails() throws Exception {
        mockMvc.perform(get("/admin/store-levels")
                        .flashAttr("errorMessage", "Không thể tải danh sách cấp độ cửa hàng!")
                        .flashAttr("dataLoadError", true))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"dataLoadAlert\"")))
                .andExpect(content().string(containsString("Không thể tải danh sách cấp độ cửa hàng!")));
    }
}
