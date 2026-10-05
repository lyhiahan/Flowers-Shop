package com.finalproject;

import com.finalproject.entity.Category;
import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.repository.CategoryRepository;
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
 * Kiểm thử toàn diện Use Case UC_030: Quản lý loại sản phẩm (System management – Category)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@SuppressWarnings("unused")
class AdminCategoryManagementUseCaseTests {

    @Test
    @WithMockUser(username = "admin_uc30", roles = {"ADMIN"})
    void deletedCategoriesDisappearFromCustomerListsAndOldLinksAndReturnAfterRestore() throws Exception {
        mockMvc.perform(post("/admin/categories/delete/" + subCat1.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection());
        var parentView = mockMvc.perform(get("/products").param("categoryId", rootCat1.getId().toString()))
                .andExpect(status().isOk()).andReturn().getModelAndView();
        assertThat((java.util.List<?>) parentView.getModel().get("subCategories")).isEmpty();

        for (Category root : java.util.List.of(rootCat1, rootCat2)) {
            mockMvc.perform(post("/admin/categories/delete/" + root.getId()).with(csrf()))
                    .andExpect(status().is3xxRedirection());
        }
        for (String path : java.util.List.of("/", "/products", "/search")) {
            var view = mockMvc.perform(get(path)).andExpect(status().isOk()).andReturn().getModelAndView();
            assertThat((java.util.List<?>) view.getModel().get("categories")).isEmpty();
            assertThat((java.util.List<?>) view.getModel().get("discoverCategories")).isEmpty();
        }
        mockMvc.perform(get("/products").param("categoryId", rootCat1.getId().toString()))
                .andExpect(redirectedUrl("/products"));
        assertThat(categoryRepository.findById(rootCat1.getId())).isPresent();

        mockMvc.perform(post("/admin/categories/restore/" + rootCat1.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/admin/categories/update/" + rootCat1.getId()).with(csrf())
                        .param("name", "Danh mục đã đổi tên").param("description", "Mô tả mới"))
                .andExpect(status().is3xxRedirection());
        var restoredView = mockMvc.perform(get("/products")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Danh mục đã đổi tên")))
                .andReturn().getModelAndView();
        assertThat((java.util.List<?>) restoredView.getModel().get("categories"))
                .extracting("id").containsExactly(rootCat1.getId());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User customer;
    private Category rootCat1;
    private Category rootCat2;
    private Category subCat1;

    @BeforeEach
    void setUp() {
        String encodedPass = passwordEncoder.encode("secret123");

        admin = userRepository.save(User.builder()
                .username("admin_uc30")
                .email("admin_uc30@example.com")
                .password(encodedPass)
                .fullName("Quản Trị Viên UC30")
                .isActive(true).role(Role.ADMIN)
                .build());

        customer = userRepository.save(User.builder()
                .username("customer_uc30")
                .email("customer_uc30@example.com")
                .password(encodedPass)
                .fullName("Khách Hàng UC30")
                .isActive(true).role(Role.USER)
                .build());

        rootCat1 = categoryRepository.save(Category.builder()
                .name("Hoa Khai Trương")
                .description("Kệ hoa chúc mừng khai trương hồng phát")
                .isDeleted(false)
                .build());

        rootCat2 = categoryRepository.save(Category.builder()
                .name("Hoa Sinh Nhật")
                .description("Bó hoa tươi mừng sinh nhật bạn bè, người thân")
                .isDeleted(false)
                .build());

        subCat1 = categoryRepository.save(Category.builder()
                .name("Kệ Hoa Khai Trương Hiện Đại")
                .description("Kệ hoa tông màu pastel sang trọng")
                .parent(rootCat1)
                .isDeleted(false)
                .build());
    }

    /**
     * Pre-condition: Chỉ người dùng có quyền Admin mới được phép truy cập
     */
    @Test
    @WithMockUser(username = "customer_uc30", roles = {"USER"})
    void testPreCondition_CustomerDeniedAccessToAdminCategories() throws Exception {
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isForbidden());
    }

    /**
     * Main Flow Bước 1 & 2: Vào trang Dashboard của Admin, chọn Category trên thanh sidebar
     */
    @Test
    @WithMockUser(username = "admin_uc30", roles = {"ADMIN"})
    void testStep1And2_NavigateFromDashboardToCategories() throws Exception {
        // Bước 1: Dashboard có link sidebar sang Category
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"sidebarCategoriesLink\"")))
                .andExpect(content().string(containsString("href=\"/admin/categories\"")));

        // Bước 2: Truy cập trang quản lý categories
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/categories"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(model().attributeExists("rootCategories"));
    }

    /**
     * Main Flow Bước 3 & 4: Hiển thị bảng danh sách category và phân trang Next / Previous
     */
    @Test
    @WithMockUser(username = "admin_uc30", roles = {"ADMIN"})
    void testStep3And4_DisplayCategoriesTableAndPagination() throws Exception {
        // Tạo thêm 10 categories để phân trang
        for (int i = 1; i <= 10; i++) {
            categoryRepository.save(Category.builder()
                    .name("Loại Sản Phẩm Thử Nghiệm " + i)
                    .description("Mô tả thử nghiệm " + i)
                    .isDeleted(false)
                    .build());
        }

        // Bước 3: Kiểm tra bảng danh sách categories
        mockMvc.perform(get("/admin/categories?page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"categoriesTable\"")))
                .andExpect(content().string(containsString("id=\"categoriesPagination\"")))
                .andExpect(content().string(containsString("id=\"btnPrevPage\"")))
                .andExpect(content().string(containsString("id=\"btnNextPage\"")))
                .andExpect(content().string(containsString("id=\"pageBtn_1\"")))
                .andExpect(content().string(containsString("id=\"pageBtn_2\"")));

        // Bước 4: Chọn Next / page 1
        mockMvc.perform(get("/admin/categories?page=1&size=5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"categoriesTable\"")))
                .andExpect(model().attribute("currentPage", 1));
    }

    /**
     * Main Flow Bước 5: Chọn nút review để xem cây category
     */
    @Test
    @WithMockUser(username = "admin_uc30", roles = {"ADMIN"})
    void testStep5_ReviewCategoryTree() throws Exception {
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isOk())
                // Kiểm tra nút review mở modal cây danh mục
                .andExpect(content().string(containsString("id=\"btnReviewCategoryTree\"")))
                .andExpect(content().string(containsString("id=\"categoryTreeModal\"")))
                // Kiểm tra nội dung cây danh mục chứa root và sub category
                .andExpect(content().string(containsString("Hoa Khai Trương")))
                .andExpect(content().string(containsString("Kệ Hoa Khai Trương Hiện Đại")));
    }

    /**
     * Main Flow Bước 6 - 12:
     * 6. Chọn nút New category để thêm mới
     * 7. Chuyển đến trang form tạo category mới
     * 8. Nhập đầy đủ thông tin
     * 9. Bấm nút Submit
     * 10. Hiển thị thông báo yêu cầu xác nhận
     * 11. Bấm nút Confirm
     * 12. Thông báo thành công
     */
    @Test
    @WithMockUser(username = "admin_uc30", roles = {"ADMIN"})
    void testStep6To12_CreateNewCategoryWithConfirmModal() throws Exception {
        // Bước 6: Trang categories có nút New category
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnNewCategory\"")))
                .andExpect(content().string(containsString("href=\"/admin/categories/create\"")));

        // Bước 7 & 8: Chuyển đến form tạo category mới
        mockMvc.perform(get("/admin/categories/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/category-form"))
                .andExpect(content().string(containsString("id=\"categoryNameInput\"")))
                .andExpect(content().string(containsString("id=\"categoryParentSelect\"")))
                .andExpect(content().string(containsString("id=\"categoryDescInput\"")))
                // Bước 9: Nút Submit
                .andExpect(content().string(containsString("id=\"btnSubmitCategory\"")))
                // Bước 10: Modal xác nhận
                .andExpect(content().string(containsString("id=\"confirmCategoryModal\"")))
                // Bước 11: Nút Confirm
                .andExpect(content().string(containsString("id=\"btnConfirmCategory\"")))
                // Alternative 11d: Nút Cancel
                .andExpect(content().string(containsString("id=\"btnCancelCategory\"")));

        // Bước 11 & 12: Submit form tạo category mới -> Thông báo thành công
        mockMvc.perform(post("/admin/categories/add")
                        .with(csrf())
                        .param("name", "Hoa Tình Yêu & Valentine")
                        .param("description", "Hoa hồng đỏ rực rỡ tặng người thương")
                        .param("parentId", rootCat2.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attributeExists("successMessage"));

        // Kiểm tra category được lưu trong database với cha tương ứng
        Category saved = categoryRepository.findAll().stream()
                .filter(c -> "Hoa Tình Yêu & Valentine".equals(c.getName()))
                .findFirst()
                .orElse(null);
        assertThat(saved).isNotNull();
        assertThat(saved.getParent()).isNotNull();
        assertThat(saved.getParent().getId()).isEqualTo(rootCat2.getId());
        assertThat(saved.getIsDeleted()).isFalse();

        // Kiểm tra hiển thị thông báo thành công trên trang categories
        mockMvc.perform(get("/admin/categories")
                        .flashAttr("successMessage", "Thêm mới loại sản phẩm [Hoa Tình Yêu & Valentine] thành công!"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"successAlert\"")))
                .andExpect(content().string(containsString("Hoa Tình Yêu")));
    }

    /**
     * Alternative Flow 6a & 7a: Chọn nút Edit tại category muốn sửa -> Chuyển đến form chỉnh sửa
     */
    @Test
    @WithMockUser(username = "admin_uc30", roles = {"ADMIN"})
    void testAlternative6aAnd7a_EditCategory() throws Exception {
        // 6a: Trang categories có nút Sửa với id btnEditCategory_{id}
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnEditCategory_" + rootCat1.getId() + "\"")))
                .andExpect(content().string(containsString("href=\"/admin/categories/edit/" + rootCat1.getId() + "\"")));

        // 7a: Truy cập trang chỉnh sửa category
        mockMvc.perform(get("/admin/categories/edit/" + rootCat1.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/category-form"))
                .andExpect(model().attribute("isEdit", true))
                .andExpect(content().string(containsString(rootCat1.getName())))
                .andExpect(content().string(containsString("id=\"btnSubmitCategory\"")))
                .andExpect(content().string(containsString("id=\"confirmCategoryModal\"")))
                .andExpect(content().string(containsString("id=\"btnConfirmCategory\"")));

        // Thực hiện cập nhật
        mockMvc.perform(post("/admin/categories/update/" + rootCat1.getId())
                        .with(csrf())
                        .param("name", "Hoa Khai Trương Cao Cấp VIP")
                        .param("description", "Cập nhật mô tả hoa khai trương VIP")
                        .param("parentId", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attributeExists("successMessage"));

        Category updated = categoryRepository.findById(rootCat1.getId()).orElse(null);
        assertThat(updated).isNotNull();
        assertThat(updated.getName()).isEqualTo("Hoa Khai Trương Cao Cấp VIP");
        assertThat(updated.getDescription()).isEqualTo("Cập nhật mô tả hoa khai trương VIP");
    }

    /**
     * Alternative Flow 6-9b & 6-9c: Xóa category (Delete) và Khôi phục category (Restore)
     */
    @Test
    @WithMockUser(username = "admin_uc30", roles = {"ADMIN"})
    void testAlternative6To9bAnd6To9c_DeleteAndRestoreCategory() throws Exception {
        // Kiểm tra nút xóa xuất hiện khi category chưa xóa
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnDeleteCategory_" + rootCat1.getId() + "\"")));

        // 6-9b: Thực hiện XÓA category (Soft delete)
        mockMvc.perform(post("/admin/categories/delete/" + rootCat1.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attributeExists("successMessage"));

        Category deleted = categoryRepository.findById(rootCat1.getId()).orElse(null);
        assertThat(deleted).isNotNull();
        assertThat(deleted.getIsDeleted()).isTrue();

        // Sau khi xóa, trang danh sách hiển thị nhãn ĐÃ XÓA và nút Khôi phục
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ĐÃ XÓA")))
                .andExpect(content().string(containsString("id=\"btnRestoreCategory_" + rootCat1.getId() + "\"")));

        // 6-9c: Thực hiện KHÔI PHỤC category
        mockMvc.perform(post("/admin/categories/restore/" + rootCat1.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attributeExists("successMessage"));

        Category restored = categoryRepository.findById(rootCat1.getId()).orElse(null);
        assertThat(restored).isNotNull();
        assertThat(restored.getIsDeleted()).isFalse();

        // Sau khi khôi phục, trang danh sách trở lại trạng thái ĐANG MỞ BÁN và có nút Xóa
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ĐANG MỞ BÁN")))
                .andExpect(content().string(containsString("id=\"btnDeleteCategory_" + rootCat1.getId() + "\"")));
    }

    /**
     * Alternative Flow 11d & 12d: Bấm nút Cancel -> Hủy thao tác
     */
    @Test
    @WithMockUser(username = "admin_uc30", roles = {"ADMIN"})
    void testAlternative11dAnd12d_CancelCreation() throws Exception {
        mockMvc.perform(get("/admin/categories/create"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnCancelCategory\"")))
                .andExpect(content().string(containsString("data-bs-dismiss=\"modal\"")));
    }

    /**
     * Post-condition: Nếu thất bại, thông báo lỗi (dataLoadAlert)
     */
    @Test
    @WithMockUser(username = "admin_uc30", roles = {"ADMIN"})
    void testPostCondition_ErrorNotificationWhenDataLoadFails() throws Exception {
        mockMvc.perform(get("/admin/categories")
                        .flashAttr("errorMessage", "Không thể tải danh sách loại sản phẩm do sự cố kết nối!")
                        .flashAttr("dataLoadError", true))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"dataLoadAlert\"")))
                .andExpect(content().string(containsString("Không thể tải danh sách loại sản phẩm do sự cố kết nối!")));
    }
}
