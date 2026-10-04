package com.finalproject;

import com.finalproject.entity.Category;
import com.finalproject.entity.Store;
import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.repository.CategoryRepository;
import com.finalproject.repository.StoreRepository;
import com.finalproject.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ManagerRoleInterfaceSyncTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User managerUser;

    @BeforeEach
    void setup() {
        Store testStore = storeRepository.save(Store.builder()
                .name("Chi Nhánh Manager Test")
                .address("123 Test Street")
                .phone("0987654321")
                .status(true)
                .isOpen(true)
                .eWallet(new BigDecimal("5000000"))
                .build());

        managerUser = userRepository.save(User.builder()
                .username("test_manager_sync")
                .password("$2a$10$abcdefghijklmnopqrstuv")
                .email("manager_sync@flower.vn")
                .fullName("Quản Lý Chi Nhánh Test")
                .role(Role.MANAGER)
                .store(testStore)
                .isActive(true)
                .build());

        // Tạo 1 root category và 1 subcategory
        Category rootCat = categoryRepository.save(Category.builder()
                .name("Hoa Tươi Cao Cấp")
                .description("Danh mục gốc")
                .build());

        categoryRepository.save(Category.builder()
                .name("Bó Hoa Tươi Cao Cấp Con")
                .description("Danh mục con")
                .parent(rootCat)
                .build());
    }

    @Test
    @WithMockUser(username = "test_manager_sync", roles = {"MANAGER", "CUSTOMER"})
    @DisplayName("1. Trang chủ hiển thị đồng bộ chế độ quản lý cửa hàng và nút vào dashboard cho Manager")
    void testIndexPage_ShowsManagerDashboardLinkAndStatus() throws Exception {
        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();
        String html = result.getResponse().getContentAsString();

        // 1. Phải có nhãn Quản lý chi nhánh
        assertThat(html).contains("Quản lý chi nhánh");

        // 2. Trạng thái: Chế độ quản lý chi nhánh (Xem website)
        assertThat(html).contains("Chế độ quản lý chi nhánh (Xem website)");

        // 3. Nút hành động trực tiếp vào kênh quản lý
        assertThat(html).contains("Vào kênh quản lý cửa hàng");
        assertThat(html).contains("/manager/dashboard");

        // 4. Thanh điều hướng nav-categories-bar có link Kênh Quản Lý
        assertThat(html).contains("Kênh Quản Lý");

        // 5. Thanh điều hướng chỉ hiển thị danh mục gốc, không bị vỡ layout
        assertThat(html).contains("Hoa Tươi Cao Cấp");
    }

    @Test
    @WithMockUser(username = "test_manager_sync", roles = {"MANAGER", "CUSTOMER"})
    @DisplayName("2. Trang hồ sơ khách hàng hiển thị nút Kênh Quản lý cửa hàng cho Manager")
    void testProfilePage_ShowsManagerDashboardLink() throws Exception {
        MvcResult result = mockMvc.perform(get("/customer/profile"))
                .andExpect(status().isOk())
                .andReturn();
        String html = result.getResponse().getContentAsString();

        assertThat(html).contains("Quản lý chi nhánh");
        assertThat(html).contains("Vào kênh quản lý cửa hàng");
        assertThat(html).contains("/manager/dashboard");
    }

    @Test
    @WithMockUser(username = "test_manager_sync", roles = {"MANAGER", "CUSTOMER"})
    @DisplayName("3. Trang danh sách cửa hàng hiển thị link Quản lý cửa hàng chuẩn role MANAGER")
    void testStoresPage_ShowsManagerDashboardLink() throws Exception {
        MvcResult result = mockMvc.perform(get("/stores"))
                .andExpect(status().isOk())
                .andReturn();
        String html = result.getResponse().getContentAsString();

        assertThat(html).contains("Kênh Quản lý cửa hàng");
        assertThat(html).contains("/manager/dashboard");
    }
}
