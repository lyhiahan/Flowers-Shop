package com.finalproject;

import com.finalproject.entity.Delivery;
import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.repository.DeliveryRepository;
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
 * Kiểm thử toàn diện Use Case UC_034: Quản lý đơn vị vận chuyển (System management – Delivery unit)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminDeliveryManagementUseCaseTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User customer;
    private Delivery delivery1;
    private Delivery delivery2;

    @BeforeEach
    void setUp() {
        String encodedPass = passwordEncoder.encode("secret123");

        admin = userRepository.save(User.builder()
                .username("admin_uc34")
                .email("admin_uc34@example.com")
                .password(encodedPass)
                .fullName("Quản Trị Viên UC34")
                .isActive(true).role(Role.ADMIN)
                .build());

        customer = userRepository.save(User.builder()
                .username("customer_uc34")
                .email("customer_uc34@example.com")
                .password(encodedPass)
                .fullName("Khách Hàng UC34")
                .isActive(true).role(Role.CUSTOMER)
                .build());

        delivery1 = deliveryRepository.save(Delivery.builder()
                .name("Giao Hàng Tiêu Chuẩn UC34")
                .price(new BigDecimal("25000"))
                .description("Giao trong ngày khu vực nội thành")
                .isActive(true)
                .isDeleted(false)
                .build());

        delivery2 = deliveryRepository.save(Delivery.builder()
                .name("GrabExpress Hỏa Tốc UC34")
                .price(new BigDecimal("45000"))
                .description("Giao nhanh trong 2H lồng kính hoa tươi")
                .isActive(true)
                .isDeleted(false)
                .build());
    }

    /**
     * Pre-condition: Chỉ người dùng có quyền Admin mới được phép truy cập
     */
    @Test
    @WithMockUser(username = "customer_uc34", roles = {"CUSTOMER"})
    void testPreCondition_CustomerDeniedAccessToAdminDeliveries() throws Exception {
        mockMvc.perform(get("/admin/deliveries"))
                .andExpect(status().isForbidden());
    }

    /**
     * Main Flow Bước 1, 2 & 3: Vào Dashboard, chọn Delivery bên sidebar, hiển thị danh sách đơn vị vận chuyển
     */
    @Test
    @WithMockUser(username = "admin_uc34", roles = {"ADMIN"})
    void testStep1To3_NavigateFromDashboardToDeliveries() throws Exception {
        // Bước 1 & 2: Dashboard có link sidebar sang Delivery
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"sidebarDeliveriesLink\"")))
                .andExpect(content().string(containsString("href=\"/admin/deliveries\"")));

        // Bước 3: Truy cập trang quản lý đơn vị vận chuyển
        mockMvc.perform(get("/admin/deliveries"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/deliveries"))
                .andExpect(model().attributeExists("deliveries"));
    }

    /**
     * Main Flow Bước 4: Hiển thị bảng danh sách đơn vị vận chuyển và phân trang Next / Previous
     */
    @Test
    @WithMockUser(username = "admin_uc34", roles = {"ADMIN"})
    void testStep4_DisplayDeliveriesTableAndPagination() throws Exception {
        // Tạo thêm các đơn vị để phân trang
        for (int i = 1; i <= 10; i++) {
            deliveryRepository.save(Delivery.builder()
                    .name("Đơn Vị Thử Nghiệm UC34 " + i)
                    .price(new BigDecimal(30000 + i * 5000))
                    .description("Mô tả thử nghiệm " + i)
                    .isActive(true)
                    .isDeleted(false)
                    .build());
        }

        // Kiểm tra bảng danh sách đơn vị vận chuyển
        mockMvc.perform(get("/admin/deliveries?page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"deliveriesTable\"")))
                .andExpect(content().string(containsString("id=\"deliveriesPagination\"")))
                .andExpect(content().string(containsString("id=\"btnPrevPage\"")))
                .andExpect(content().string(containsString("id=\"btnNextPage\"")))
                .andExpect(content().string(containsString("id=\"pageBtn_1\"")))
                .andExpect(content().string(containsString("id=\"pageBtn_2\"")));

        // Chọn Next / page 1
        mockMvc.perform(get("/admin/deliveries?page=1&size=5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"deliveriesTable\"")))
                .andExpect(model().attribute("currentPage", 1));
    }

    /**
     * Main Flow Bước 5 - 11:
     * 5. Chọn nút New delivery unit để thêm mới
     * 6. Hiển thị form thêm mới
     * 7. Nhập đầy đủ thông tin
     * 8. Bấm nút Submit
     * 9. Hiển thị thông báo yêu cầu xác nhận
     * 10. Bấm nút Confirm
     * 11. Thông báo thành công
     */
    @Test
    @WithMockUser(username = "admin_uc34", roles = {"ADMIN"})
    void testStep5To11_CreateNewDeliveryWithConfirmModal() throws Exception {
        // Bước 5: Trang deliveries có nút New delivery unit
        mockMvc.perform(get("/admin/deliveries"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnNewDelivery\"")))
                .andExpect(content().string(containsString("href=\"/admin/deliveries/create\"")));

        // Bước 6 & 7: Chuyển đến form thêm mới
        mockMvc.perform(get("/admin/deliveries/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/delivery-form"))
                .andExpect(content().string(containsString("id=\"deliveryNameInput\"")))
                .andExpect(content().string(containsString("id=\"deliveryPriceInput\"")))
                .andExpect(content().string(containsString("id=\"deliveryDescInput\"")))
                // Bước 8: Nút Submit
                .andExpect(content().string(containsString("id=\"btnSubmitDelivery\"")))
                // Bước 9: Modal xác nhận
                .andExpect(content().string(containsString("id=\"confirmDeliveryModal\"")))
                // Bước 10: Nút Confirm
                .andExpect(content().string(containsString("id=\"btnConfirmDelivery\"")))
                // Alternative 10d: Nút Cancel
                .andExpect(content().string(containsString("id=\"btnCancelDelivery\"")));

        // Bước 10 & 11: Submit form thêm đơn vị vận chuyển mới -> Thông báo thành công
        mockMvc.perform(post("/admin/deliveries/add")
                        .with(csrf())
                        .param("name", "Ahamove Siêu Tốc UC34")
                        .param("price", "50000")
                        .param("description", "Giao hoa siêu tốc trong 1 giờ"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/deliveries"))
                .andExpect(flash().attributeExists("successMessage"));

        // Kiểm tra đơn vị được lưu trong cơ sở dữ liệu
        Delivery saved = deliveryRepository.findAll().stream()
                .filter(d -> "Ahamove Siêu Tốc UC34".equals(d.getName()))
                .findFirst()
                .orElse(null);
        assertThat(saved).isNotNull();
        assertThat(saved.getPrice()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(saved.getDescription()).isEqualTo("Giao hoa siêu tốc trong 1 giờ");
        assertThat(saved.getIsDeleted()).isFalse();

        // Kiểm tra hiển thị thông báo thành công trên trang danh sách
        mockMvc.perform(get("/admin/deliveries")
                        .flashAttr("successMessage", "Thêm đơn vị vận chuyển [Ahamove Siêu Tốc UC34] thành công!"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"successAlert\"")))
                .andExpect(content().string(containsString("Ahamove Siêu Tốc UC34")));
    }

    /**
     * Alternative Flow 5a & 6a: Chọn nút Edit để chỉnh sửa -> Hiển thị form chỉnh sửa
     */
    @Test
    @WithMockUser(username = "admin_uc34", roles = {"ADMIN"})
    void testAlternative5aAnd6a_EditDelivery() throws Exception {
        // 5a: Trang danh sách có nút Edit với id btnEditDelivery_{id}
        mockMvc.perform(get("/admin/deliveries"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnEditDelivery_" + delivery2.getId() + "\"")))
                .andExpect(content().string(containsString("href=\"/admin/deliveries/edit/" + delivery2.getId() + "\"")));

        // 6a: Truy cập form chỉnh sửa đơn vị vận chuyển
        mockMvc.perform(get("/admin/deliveries/edit/" + delivery2.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/delivery-form"))
                .andExpect(model().attribute("isEdit", true))
                .andExpect(content().string(containsString(delivery2.getName())))
                .andExpect(content().string(containsString("id=\"btnSubmitDelivery\"")))
                .andExpect(content().string(containsString("id=\"confirmDeliveryModal\"")))
                .andExpect(content().string(containsString("id=\"btnConfirmDelivery\"")));

        // Thực hiện cập nhật
        mockMvc.perform(post("/admin/deliveries/update/" + delivery2.getId())
                        .with(csrf())
                        .param("name", "GrabExpress Hỏa Tốc VIP UC34")
                        .param("price", "55000")
                        .param("description", "Giao hoa hỏa tốc bảo quản thùng lạnh cao cấp"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/deliveries"))
                .andExpect(flash().attributeExists("successMessage"));

        Delivery updated = deliveryRepository.findById(delivery2.getId()).orElse(null);
        assertThat(updated).isNotNull();
        assertThat(updated.getName()).isEqualTo("GrabExpress Hỏa Tốc VIP UC34");
        assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("55000"));
        assertThat(updated.getDescription()).isEqualTo("Giao hoa hỏa tốc bảo quản thùng lạnh cao cấp");
    }

    /**
     * Alternative Flow 5-8b & 5-8c: Xóa (Delete) và Khôi phục (Restore) đơn vị vận chuyển
     */
    @Test
    @WithMockUser(username = "admin_uc34", roles = {"ADMIN"})
    void testAlternative5To8bAnd5To8c_DeleteAndRestoreDelivery() throws Exception {
        // Kiểm tra nút xóa xuất hiện khi đơn vị chưa xóa
        mockMvc.perform(get("/admin/deliveries"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnDeleteDelivery_" + delivery1.getId() + "\"")));

        // 5-8b: Thực hiện XÓA đơn vị vận chuyển (soft delete)
        mockMvc.perform(post("/admin/deliveries/delete/" + delivery1.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/deliveries"))
                .andExpect(flash().attributeExists("successMessage"));

        Delivery deleted = deliveryRepository.findById(delivery1.getId()).orElse(null);
        assertThat(deleted).isNotNull();
        assertThat(deleted.getIsDeleted()).isTrue();

        // Sau khi xóa, trang danh sách hiển thị nhãn Đã xóa và nút Khôi phục
        mockMvc.perform(get("/admin/deliveries"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Đã xóa")))
                .andExpect(content().string(containsString("id=\"btnRestoreDelivery_" + delivery1.getId() + "\"")));

        // 5-8c: Thực hiện KHÔI PHỤC đơn vị vận chuyển
        mockMvc.perform(post("/admin/deliveries/restore/" + delivery1.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/deliveries"))
                .andExpect(flash().attributeExists("successMessage"));

        Delivery restored = deliveryRepository.findById(delivery1.getId()).orElse(null);
        assertThat(restored).isNotNull();
        assertThat(restored.getIsDeleted()).isFalse();

        // Sau khi khôi phục, trang danh sách trở lại trạng thái Đang hoạt động và có nút Xóa
        mockMvc.perform(get("/admin/deliveries"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Đang hoạt động")))
                .andExpect(content().string(containsString("id=\"btnDeleteDelivery_" + delivery1.getId() + "\"")));
    }

    /**
     * Alternative Flow 10d & 11d: Bấm nút Cancel -> Hủy thao tác
     */
    @Test
    @WithMockUser(username = "admin_uc34", roles = {"ADMIN"})
    void testAlternative10dAnd11d_CancelActions() throws Exception {
        mockMvc.perform(get("/admin/deliveries/create"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnCancelDelivery\"")))
                .andExpect(content().string(containsString("data-bs-dismiss=\"modal\"")));
    }

    /**
     * Post-condition: Nếu thất bại, thông báo lỗi (dataLoadAlert)
     */
    @Test
    @WithMockUser(username = "admin_uc34", roles = {"ADMIN"})
    void testPostCondition_ErrorNotificationWhenDataLoadFails() throws Exception {
        mockMvc.perform(get("/admin/deliveries")
                        .flashAttr("errorMessage", "Không thể tải danh sách đơn vị vận chuyển!")
                        .flashAttr("dataLoadError", true))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"dataLoadAlert\"")))
                .andExpect(content().string(containsString("Không thể tải danh sách đơn vị vận chuyển!")));
    }
}
