package com.finalproject;

import com.finalproject.entity.Category;
import com.finalproject.entity.Style;
import com.finalproject.entity.StyleValue;
import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.repository.CategoryRepository;
import com.finalproject.repository.StyleRepository;
import com.finalproject.repository.StyleValueRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Kiểm thử toàn diện Use Case UC_031: Quản lý thuộc tính sản phẩm (System management – Style)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@SuppressWarnings("unused")
class AdminStyleManagementUseCaseTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private StyleRepository styleRepository;

    @Autowired
    private StyleValueRepository styleValueRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User customer;
    private Category category;
    private Style style1;
    private Style style2;
    private StyleValue value1;

    @BeforeEach
    void setUp() {
        String encodedPass = passwordEncoder.encode("secret123");

        admin = userRepository.save(User.builder()
                .username("admin_uc31")
                .email("admin_uc31@example.com")
                .password(encodedPass)
                .fullName("Quản Trị Viên UC31")
                .isActive(true).role(Role.ADMIN)
                .build());

        customer = userRepository.save(User.builder()
                .username("customer_uc31")
                .email("customer_uc31@example.com")
                .password(encodedPass)
                .fullName("Khách Hàng UC31")
                .isActive(true).role(Role.CUSTOMER)
                .build());

        category = categoryRepository.save(Category.builder()
                .name("Hoa Tươi Thiết Kế UC31")
                .description("Danh mục thử nghiệm UC31")
                .isDeleted(false)
                .build());

        style1 = styleRepository.save(Style.builder()
                .name("Tone Màu Hoa UC31")
                .category(category)
                .isDeleted(false)
                .build());

        style2 = styleRepository.save(Style.builder()
                .name("Kiểu Dáng Thiết Kế UC31")
                .isDeleted(false)
                .build());

        value1 = styleValueRepository.save(StyleValue.builder()
                .name("Pastel Ngọt Ngào")
                .style(style1)
                .isDeleted(false)
                .build());
    }

    /**
     * Pre-condition: Chỉ người dùng có quyền Admin mới được phép truy cập
     */
    @Test
    @WithMockUser(username = "customer_uc31", roles = {"CUSTOMER"})
    void testPreCondition_CustomerDeniedAccessToAdminStyles() throws Exception {
        mockMvc.perform(get("/admin/styles"))
                .andExpect(status().isForbidden());
    }

    /**
     * Main Flow Bước 1 & 2: Vào trang Dashboard của Admin, chọn Style trên thanh sidebar
     */
    @Test
    @WithMockUser(username = "admin_uc31", roles = {"ADMIN"})
    void testStep1And2_NavigateFromDashboardToStyles() throws Exception {
        // Bước 1: Dashboard có link sidebar sang Style
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"sidebarStylesLink\"")))
                .andExpect(content().string(containsString("href=\"/admin/styles\"")));

        // Bước 2: Truy cập trang quản lý styles
        mockMvc.perform(get("/admin/styles"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/styles"))
                .andExpect(model().attributeExists("styles"));
    }

    /**
     * Main Flow Bước 3 & 4: Hiển thị bảng danh sách style và phân trang Next / Previous
     */
    @Test
    @WithMockUser(username = "admin_uc31", roles = {"ADMIN"})
    void testStep3And4_DisplayStylesTableAndPagination() throws Exception {
        // Tạo thêm 10 styles để phân trang
        for (int i = 1; i <= 10; i++) {
            styleRepository.save(Style.builder()
                    .name("Thuộc Tính Phân Trang " + i)
                    .isDeleted(false)
                    .build());
        }

        // Bước 3: Kiểm tra bảng danh sách styles
        mockMvc.perform(get("/admin/styles?page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"stylesTable\"")))
                .andExpect(content().string(containsString("id=\"stylesPagination\"")))
                .andExpect(content().string(containsString("id=\"btnPrevPage\"")))
                .andExpect(content().string(containsString("id=\"btnNextPage\"")))
                .andExpect(content().string(containsString("id=\"pageBtn_1\"")))
                .andExpect(content().string(containsString("id=\"pageBtn_2\"")));

        // Bước 4: Chọn Next / page 1
        mockMvc.perform(get("/admin/styles?page=1&size=5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"stylesTable\"")))
                .andExpect(model().attribute("currentPage", 1));
    }

    /**
     * Main Flow Bước 5 - 11:
     * 5. Chọn nút New style để thêm mới
     * 6. Chuyển đến trang form tạo style mới
     * 7. Nhập đầy đủ thông tin
     * 8. Bấm nút Submit
     * 9. Hiển thị thông báo yêu cầu xác nhận
     * 10. Bấm nút Confirm
     * 11. Thông báo thành công
     */
    @Test
    @WithMockUser(username = "admin_uc31", roles = {"ADMIN"})
    void testStep5To11_CreateNewStyleWithConfirmModal() throws Exception {
        // Bước 5: Trang styles có nút New style
        mockMvc.perform(get("/admin/styles"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnNewStyle\"")))
                .andExpect(content().string(containsString("href=\"/admin/styles/create\"")));

        // Bước 6 & 7: Chuyển đến form tạo style mới
        mockMvc.perform(get("/admin/styles/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/style-form"))
                .andExpect(content().string(containsString("id=\"styleNameInput\"")))
                .andExpect(content().string(containsString("id=\"styleCategorySelect\"")))
                // Bước 8: Nút Submit
                .andExpect(content().string(containsString("id=\"btnSubmitStyle\"")))
                // Bước 9: Modal xác nhận
                .andExpect(content().string(containsString("id=\"confirmStyleModal\"")))
                // Bước 10: Nút Confirm
                .andExpect(content().string(containsString("id=\"btnConfirmStyle\"")))
                // Alternative 11h: Nút Cancel
                .andExpect(content().string(containsString("id=\"btnCancelStyle\"")));

        // Bước 10 & 11: Submit form tạo style mới -> Thông báo thành công
        mockMvc.perform(post("/admin/styles/add")
                        .with(csrf())
                        .param("name", "Kích Cỡ Bó Hoa Mới")
                        .param("categoryId", category.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/styles"))
                .andExpect(flash().attributeExists("successMessage"));

        // Kiểm tra style được lưu trong cơ sở dữ liệu
        Style saved = styleRepository.findAll().stream()
                .filter(s -> "Kích Cỡ Bó Hoa Mới".equals(s.getName()))
                .findFirst()
                .orElse(null);
        assertThat(saved).isNotNull();
        assertThat(saved.getCategory()).isNotNull();
        assertThat(saved.getCategory().getId()).isEqualTo(category.getId());
        assertThat(saved.getIsDeleted()).isFalse();

        // Kiểm tra hiển thị thông báo thành công trên trang styles
        mockMvc.perform(get("/admin/styles")
                        .flashAttr("successMessage", "Thêm mới thuộc tính [Kích Cỡ Bó Hoa Mới] thành công!"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"successAlert\"")))
                .andExpect(content().string(containsString("Kích Cỡ Bó Hoa Mới")));
    }

    /**
     * Alternative Flow 5a & 6a: Chọn nút Edit tại style muốn sửa -> Chuyển đến form chỉnh sửa
     */
    @Test
    @WithMockUser(username = "admin_uc31", roles = {"ADMIN"})
    void testAlternative5aAnd6a_EditStyle() throws Exception {
        // 5a: Trang styles có nút Sửa với id btnEditStyle_{id}
        mockMvc.perform(get("/admin/styles"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnEditStyle_" + style1.getId() + "\"")))
                .andExpect(content().string(containsString("href=\"/admin/styles/edit/" + style1.getId() + "\"")));

        // 6a: Truy cập trang chỉnh sửa style
        mockMvc.perform(get("/admin/styles/edit/" + style1.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/style-form"))
                .andExpect(model().attribute("isEdit", true))
                .andExpect(content().string(containsString(style1.getName())))
                .andExpect(content().string(containsString("id=\"btnSubmitStyle\"")))
                .andExpect(content().string(containsString("id=\"confirmStyleModal\"")))
                .andExpect(content().string(containsString("id=\"btnConfirmStyle\"")));

        // Thực hiện cập nhật
        mockMvc.perform(post("/admin/styles/update/" + style1.getId())
                        .with(csrf())
                        .param("name", "Tone Màu Thiết Kế VIP")
                        .param("categoryId", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/styles"))
                .andExpect(flash().attributeExists("successMessage"));

        Style updated = styleRepository.findById(style1.getId()).orElse(null);
        assertThat(updated).isNotNull();
        assertThat(updated.getName()).isEqualTo("Tone Màu Thiết Kế VIP");
        assertThat(updated.getCategory()).isNull();
    }

    /**
     * Alternative Flow 5-9b & 5-9c: Xóa style (Delete) và Khôi phục style (Restore)
     */
    @Test
    @WithMockUser(username = "admin_uc31", roles = {"ADMIN"})
    void testAlternative5To9bAnd5To9c_DeleteAndRestoreStyle() throws Exception {
        // Kiểm tra nút xóa xuất hiện khi style chưa xóa
        mockMvc.perform(get("/admin/styles"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnDeleteStyle_" + style2.getId() + "\"")));

        // 5-9b: Thực hiện XÓA style (Soft delete)
        mockMvc.perform(post("/admin/styles/delete/" + style2.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/styles"))
                .andExpect(flash().attributeExists("successMessage"));

        Style deleted = styleRepository.findById(style2.getId()).orElse(null);
        assertThat(deleted).isNotNull();
        assertThat(deleted.getIsDeleted()).isTrue();

        // Sau khi xóa, trang danh sách hiển thị nhãn ĐÃ XÓA và nút Khôi phục
        mockMvc.perform(get("/admin/styles"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ĐÃ XÓA")))
                .andExpect(content().string(containsString("id=\"btnRestoreStyle_" + style2.getId() + "\"")));

        // 5-9c: Thực hiện KHÔI PHỤC style
        mockMvc.perform(post("/admin/styles/restore/" + style2.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/styles"))
                .andExpect(flash().attributeExists("successMessage"));

        Style restored = styleRepository.findById(style2.getId()).orElse(null);
        assertThat(restored).isNotNull();
        assertThat(restored.getIsDeleted()).isFalse();

        // Sau khi khôi phục, trang danh sách trở lại trạng thái ĐANG MỞ BÁN và có nút Xóa
        mockMvc.perform(get("/admin/styles"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ĐANG MỞ BÁN")))
                .andExpect(content().string(containsString("id=\"btnDeleteStyle_" + style2.getId() + "\"")));
    }

    /**
     * Alternative Flow 5d, 6d1 - 6d3: Xem chi tiết giá trị (Detail) và Thêm giá trị mới (New value)
     */
    @Test
    @WithMockUser(username = "admin_uc31", roles = {"ADMIN"})
    void testAlternative5dAnd6d1To6d3_DetailAndAddNewStyleValue() throws Exception {
        // 5d: Bấm nút Detail của style1
        mockMvc.perform(get("/admin/styles"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnDetailStyle_" + style1.getId() + "\"")))
                .andExpect(content().string(containsString("href=\"/admin/styles/" + style1.getId() + "/values\"")));

        // 6d1: Chuyển đến trang danh sách giá trị thuộc tính
        mockMvc.perform(get("/admin/styles/" + style1.getId() + "/values"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/style-values"))
                .andExpect(content().string(containsString("id=\"styleValuesTable\"")))
                .andExpect(content().string(containsString(value1.getName())))
                // 6d2: Nút New value
                .andExpect(content().string(containsString("id=\"btnNewStyleValue\"")))
                // 6d3: Modal thêm mới style value
                .andExpect(content().string(containsString("id=\"addStyleValueModal\"")))
                .andExpect(content().string(containsString("id=\"styleValueNameInput\"")))
                .andExpect(content().string(containsString("id=\"btnSubmitStyleValue\"")))
                .andExpect(content().string(containsString("id=\"btnCancelStyleValue\"")));

        // Thêm giá trị mới
        mockMvc.perform(post("/admin/styles/" + style1.getId() + "/values/add")
                        .with(csrf())
                        .param("name", "Đỏ Rực Rỡ"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/styles/" + style1.getId() + "/values"))
                .andExpect(flash().attributeExists("successMessage"));

        StyleValue savedVal = styleValueRepository.findAll().stream()
                .filter(v -> "Đỏ Rực Rỡ".equals(v.getName()))
                .findFirst()
                .orElse(null);
        assertThat(savedVal).isNotNull();
        assertThat(savedVal.getStyle().getId()).isEqualTo(style1.getId());
        assertThat(savedVal.getIsDeleted()).isFalse();
    }

    /**
     * Alternative Flow 6e2, 6e3, 6f2-9, 6g2-9: Sửa, Xóa và Khôi phục Style Value
     */
    @Test
    @WithMockUser(username = "admin_uc31", roles = {"ADMIN"})
    void testAlternative6e2To6g_EditDeleteAndRestoreStyleValue() throws Exception {
        // Kiểm tra nút Sửa và Xóa của value1
        mockMvc.perform(get("/admin/styles/" + style1.getId() + "/values"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnEditStyleValue_" + value1.getId() + "\"")))
                .andExpect(content().string(containsString("id=\"btnDeleteStyleValue_" + value1.getId() + "\"")));

        // 6e2 & 6e3: Sửa style value
        mockMvc.perform(post("/admin/styles/" + style1.getId() + "/values/update/" + value1.getId())
                        .with(csrf())
                        .param("name", "Pastel Nhẹ Nhàng Cổ Điển"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/styles/" + style1.getId() + "/values"))
                .andExpect(flash().attributeExists("successMessage"));

        StyleValue updatedVal = styleValueRepository.findById(value1.getId()).orElse(null);
        assertThat(updatedVal).isNotNull();
        assertThat(updatedVal.getName()).isEqualTo("Pastel Nhẹ Nhàng Cổ Điển");

        // 6f2-9: Xóa style value
        mockMvc.perform(post("/admin/styles/" + style1.getId() + "/values/delete/" + value1.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/styles/" + style1.getId() + "/values"))
                .andExpect(flash().attributeExists("successMessage"));

        StyleValue deletedVal = styleValueRepository.findById(value1.getId()).orElse(null);
        assertThat(deletedVal).isNotNull();
        assertThat(deletedVal.getIsDeleted()).isTrue();

        // Kiểm tra nút khôi phục hiển thị sau khi xóa
        mockMvc.perform(get("/admin/styles/" + style1.getId() + "/values"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnRestoreStyleValue_" + value1.getId() + "\"")));

        // 6g2-9: Khôi phục style value
        mockMvc.perform(post("/admin/styles/" + style1.getId() + "/values/restore/" + value1.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/styles/" + style1.getId() + "/values"))
                .andExpect(flash().attributeExists("successMessage"));

        StyleValue restoredVal = styleValueRepository.findById(value1.getId()).orElse(null);
        assertThat(restoredVal).isNotNull();
        assertThat(restoredVal.getIsDeleted()).isFalse();
    }

    /**
     * Alternative Flow 11h & 12h: Bấm nút Cancel -> Hủy thao tác
     */
    @Test
    @WithMockUser(username = "admin_uc31", roles = {"ADMIN"})
    void testAlternative11hAnd12h_CancelActions() throws Exception {
        // Form tạo style có nút cancel
        mockMvc.perform(get("/admin/styles/create"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnCancelStyle\"")))
                .andExpect(content().string(containsString("data-bs-dismiss=\"modal\"")));

        // Modal tạo style value có nút cancel
        mockMvc.perform(get("/admin/styles/" + style1.getId() + "/values"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnCancelStyleValue\"")))
                .andExpect(content().string(containsString("data-bs-dismiss=\"modal\"")));
    }

    /**
     * Post-condition: Nếu thất bại, thông báo lỗi (dataLoadAlert)
     */
    @Test
    @WithMockUser(username = "admin_uc31", roles = {"ADMIN"})
    void testPostCondition_ErrorNotificationWhenDataLoadFails() throws Exception {
        mockMvc.perform(get("/admin/styles")
                        .flashAttr("errorMessage", "Không thể tải danh sách thuộc tính sản phẩm!")
                        .flashAttr("dataLoadError", true))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"dataLoadAlert\"")))
                .andExpect(content().string(containsString("Không thể tải danh sách thuộc tính sản phẩm!")));
    }
}
