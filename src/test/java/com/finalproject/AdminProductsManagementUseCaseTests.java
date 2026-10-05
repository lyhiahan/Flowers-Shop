package com.finalproject;

import com.finalproject.entity.Category;
import com.finalproject.entity.Product;
import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.repository.CategoryRepository;
import com.finalproject.repository.ProductRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Kiểm thử Use Case UC_028: Quản lý sản phẩm trong hệ thống (Products management)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@SuppressWarnings("unused")
class AdminProductsManagementUseCaseTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.finalproject.repository.StoreRepository storeRepository;

    @Autowired
    private com.finalproject.repository.StoreInventoryRepository storeInventoryRepository;

    private User admin;
    private com.finalproject.entity.Store store1;
    private Category category;
    private Product licensedProduct1;
    private Product licensedProduct2;
    private Product licensedProduct3;
    private Product unlicensedProduct1;

    @BeforeEach
    void setUp() {
        String encodedPass = passwordEncoder.encode("secret123");

        admin = userRepository.save(User.builder()
                .username("admin_uc28")
                .email("admin_uc28@example.com")
                .password(encodedPass)
                .fullName("Quản Trị Viên UC28")
                .role(Role.ADMIN)
                .isActive(true)
                .createdDate(LocalDate.now())
                .build());

        category = categoryRepository.save(Category.builder()
                .name("Hoa Tươi Sài Gòn UC28")
                .build());

        // Sản phẩm được cấp phép (Licensed)
        licensedProduct1 = productRepository.save(Product.builder()
                .name("Bó Hoa Hồng Đỏ Cấp Phép UC28")
                .category(category)
                .importPrice(new BigDecimal("150000"))
                .priceBuy(new BigDecimal("250000"))
                .priceRentPerDay(new BigDecimal("40000"))
                .depositFee(new BigDecimal("80000"))
                .sold(10)
                .isActive(true)
                .isSelling(true)
                .createdAt(LocalDateTime.now())
                .build());

        licensedProduct2 = productRepository.save(Product.builder()
                .name("Lẵng Hoa Lan Hồ Điệp Cấp Phép UC28")
                .category(category)
                .importPrice(new BigDecimal("300000"))
                .priceBuy(new BigDecimal("500000"))
                .priceRentPerDay(new BigDecimal("70000"))
                .depositFee(new BigDecimal("150000"))
                .sold(5)
                .isActive(true)
                .isSelling(true)
                .createdAt(LocalDateTime.now())
                .build());

        licensedProduct3 = productRepository.save(Product.builder()
                .name("Giỏ Hoa Baby Trắng Cấp Phép UC28")
                .category(category)
                .importPrice(new BigDecimal("100000"))
                .priceBuy(new BigDecimal("180000"))
                .priceRentPerDay(new BigDecimal("30000"))
                .depositFee(new BigDecimal("60000"))
                .sold(8)
                .isActive(true)
                .isSelling(true)
                .createdAt(LocalDateTime.now())
                .build());

        store1 = storeRepository.save(com.finalproject.entity.Store.builder()
                .name("Chi nhánh Bến Thành UC28")
                .address("123 Lê Lợi, Quận 1")
                .phone("0901234567")
                .status(true)
                .isOpen(true)
                .build());

        // Sản phẩm chưa được cấp phép hoặc bị cấm (Unlicensed / Banned)
        unlicensedProduct1 = productRepository.save(Product.builder()
                .name("Hoa Ngoại Nhập Bị Cấm UC28")
                .category(category)
                .importPrice(new BigDecimal("400000"))
                .priceBuy(new BigDecimal("700000"))
                .priceRentPerDay(new BigDecimal("90000"))
                .depositFee(new BigDecimal("200000"))
                .sold(0)
                .isActive(false)
                .isSelling(false)
                .createdAt(LocalDateTime.now())
                .build());

        // Thiết lập tồn kho minh họa:
        // licensedProduct1: 24 bó (Còn hàng)
        // licensedProduct2: 3 lẵng (Sắp hết)
        // unlicensedProduct1: 0 bó (Hết hàng)
        storeInventoryRepository.save(com.finalproject.entity.StoreInventory.builder()
                .store(store1).product(licensedProduct1).quantity(24).build());
        storeInventoryRepository.save(com.finalproject.entity.StoreInventory.builder()
                .store(store1).product(licensedProduct2).quantity(3).build());
        storeInventoryRepository.save(com.finalproject.entity.StoreInventory.builder()
                .store(store1).product(unlicensedProduct1).quantity(0).build());
    }

    /**
     * Pre-condition: Chỉ người dùng có vai trò ADMIN mới được truy cập vào trang Quản lý sản phẩm.
     * Người dùng vai trò USER sẽ bị từ chối truy cập (403 Forbidden).
     */
    @Test
    @WithMockUser(username = "customer_denied_uc28", roles = {"USER"})
    void testPreCondition_CustomerDeniedAccessToAdminProducts() throws Exception {
        mockMvc.perform(get("/admin/products-manage"))
                .andExpect(status().isForbidden());
    }

    /**
     * Main Flow Bước 1 & 2:
     * 1. Vào trang Dashboard của Admin (/admin/dashboard)
     * 2. Chọn Products bên thanh sidebar (id="sidebarProductsLink" dẫn tới /admin/products-manage)
     */
    @Test
    @WithMockUser(username = "admin_uc28", roles = {"ADMIN"})
    void testStep1And2_NavigateFromDashboardToProducts() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(content().string(containsString("id=\"sidebarProductsLink\"")))
                .andExpect(content().string(containsString("href=\"/admin/products-manage\"")));
    }

    /**
     * Main Flow Bước 3:
     * 3. Chọn Licensed products (hoặc Unlicensed products) để xem sản phẩm được cấp phép (bị cấm)
     */
    @Test
    @WithMockUser(username = "admin_uc28", roles = {"ADMIN"})
    void testStep3_SelectLicensedAndUnlicensedProductsTabs() throws Exception {
        // Tab Licensed products: Chỉ hiển thị các sản phẩm được cấp phép (isActive = true)
        mockMvc.perform(get("/admin/products-manage")
                        .param("tab", "LICENSED"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/products-manage"))
                .andExpect(model().attribute("currentTab", "LICENSED"))
                .andExpect(content().string(containsString("id=\"tabLicensedProducts\"")))
                .andExpect(content().string(containsString("id=\"tabUnlicensedProducts\"")))
                .andExpect(content().string(containsString("Bó Hoa Hồng Đỏ Cấp Phép UC28")))
                .andExpect(content().string(not(containsString("Hoa Ngoại Nhập Bị Cấm UC28"))));

        // Tab Unlicensed products: Chỉ hiển thị các sản phẩm bị cấm / chưa được cấp phép (isActive = false)
        mockMvc.perform(get("/admin/products-manage")
                        .param("tab", "UNLICENSED"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/products-manage"))
                .andExpect(model().attribute("currentTab", "UNLICENSED"))
                .andExpect(content().string(containsString("id=\"tabLicensedProducts\"")))
                .andExpect(content().string(containsString("id=\"tabUnlicensedProducts\"")))
                .andExpect(content().string(containsString("Hoa Ngoại Nhập Bị Cấm UC28")))
                .andExpect(content().string(not(containsString("Bó Hoa Hồng Đỏ Cấp Phép UC28"))));
    }

    /**
     * Main Flow Bước 4:
     * 4. Hiển thị bảng danh sách sản phẩm (id="productsTable")
     * Post-condition (thành công): Hiển thị bảng danh sách tất cả các sản phẩm trong hệ thống
     */
    @Test
    @WithMockUser(username = "admin_uc28", roles = {"ADMIN"})
    void testStep4_DisplayProductsTable() throws Exception {
        mockMvc.perform(get("/admin/products-manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/products-manage"))
                .andExpect(model().attributeExists("products"))
                .andExpect(model().attributeExists("totalPages"))
                .andExpect(model().attributeExists("totalElements"))
                .andExpect(model().attributeExists("currentPage"))
                .andExpect(content().string(containsString("id=\"productsTable\"")))
                .andExpect(content().string(containsString("Quản lý sản phẩm")))
                .andExpect(content().string(containsString("Bó Hoa Hồng Đỏ Cấp Phép UC28")))
                .andExpect(content().string(containsString("Lẵng Hoa Lan Hồ Điệp Cấp Phép UC28")));
    }

    /**
     * Main Flow Bước 5:
     * 5. Chọn Next (hoặc Previous, hoặc trang bất kỳ) để xem trang tiếp theo (hoặc trước đó, hoặc trang tương ứng)
     */
    @Test
    @WithMockUser(username = "admin_uc28", roles = {"ADMIN"})
    void testStep5_PaginationNextPreviousAndDirectPage() throws Exception {
        // Truy cập trang đầu tiên với kích thước trang là 2 (tổng cộng ít nhất 4 sản phẩm tạo ra nhiều trang)
        mockMvc.perform(get("/admin/products-manage")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/products-manage"))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(content().string(containsString("id=\"productsPagination\"")))
                .andExpect(content().string(containsString("id=\"btnNextPage\"")))
                .andExpect(content().string(containsString("id=\"btnPrevPage\"")))
                .andExpect(content().string(containsString("id=\"pageBtn_1\"")))
                .andExpect(content().string(containsString("id=\"pageBtn_2\"")));

        // Bấm Next (chọn trang tiếp theo: page=1)
        mockMvc.perform(get("/admin/products-manage")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/products-manage"))
                .andExpect(model().attribute("currentPage", 1))
                .andExpect(content().string(containsString("id=\"productsPagination\"")));
    }

    /**
     * Post-condition (thất bại): Nếu thất bại hoặc có lỗi tải dữ liệu, thông báo lỗi được hiển thị.
     */
    @Test
    @WithMockUser(username = "admin_uc28", roles = {"ADMIN"})
    void testPostCondition_ErrorNotificationWhenDataLoadFails() throws Exception {
        mockMvc.perform(get("/admin/products-manage")
                        .flashAttr("dataLoadError", true)
                        .flashAttr("errorMessage", "Không thể tải danh sách sản phẩm. Vui lòng thử lại!"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"dataLoadAlert\"")))
                .andExpect(content().string(containsString("Không thể tải danh sách sản phẩm. Vui lòng thử lại!")));
    }

    /**
     * Kiểm thử hiển thị cột thao tác cố định sticky-col-right và đầy đủ các nút [Chi tiết], [Sửa], [⋯]
     */
    @Test
    @WithMockUser(username = "admin_uc28", roles = {"ADMIN"})
    void testActionColumn_DisplaysStickyAndAllActions() throws Exception {
        mockMvc.perform(get("/admin/products-manage"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("sticky-col-right")))
                .andExpect(content().string(containsString("Chi tiết")))
                .andExpect(content().string(containsString("Sửa")))
                .andExpect(content().string(containsString("bi-three-dots")))
                .andExpect(content().string(containsString("Ngừng bán")))
                .andExpect(content().string(containsString("Xóa sản phẩm")));
    }

    /**
     * Kiểm thử luồng Sửa sản phẩm: Mở biểu mẫu điền sẵn thông tin hiện tại, số lượng đã bán readonly
     */
    @Test
    @WithMockUser(username = "admin_uc28", roles = {"ADMIN"})
    void testEditProduct_ShowFormWithExistingData() throws Exception {
        mockMvc.perform(get("/admin/products-manage/edit/" + licensedProduct1.getId())
                        .param("returnUrl", "/admin/products-manage?page=0&tab=ALL"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product-form"))
                .andExpect(model().attribute("isEdit", true))
                .andExpect(model().attributeExists("productForm"))
                .andExpect(model().attribute("productForm", org.hamcrest.Matchers.hasProperty("name", org.hamcrest.Matchers.equalTo(licensedProduct1.getName()))))
                .andExpect(model().attribute("productForm", org.hamcrest.Matchers.hasProperty("sold", org.hamcrest.Matchers.equalTo(10))))
                .andExpect(content().string(containsString("Chỉnh Sửa Sản Phẩm #" + licensedProduct1.getId())))
                .andExpect(content().string(containsString("Hoa Hồng Đỏ")))
                .andExpect(content().string(containsString("10 s")))
                .andExpect(content().string(containsString("Lưu thay đổi")));
    }

    /**
     * Kiểm thử luồng Sửa sản phẩm: Lưu thay đổi và redirect về đúng returnUrl
     */
    @Test
    @WithMockUser(username = "admin_uc28", roles = {"ADMIN"})
    void testEditProduct_SaveUpdatesAndRedirectToReturnUrl() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin/products-manage/edit/" + licensedProduct1.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .param("name", "Bó Hoa Hồng Đỏ Đã Cập Nhật")
                        .param("categoryId", category.getId().toString())
                        .param("importPrice", "180000")
                        .param("priceBuy", "290000")
                        .param("description", "Mô tả mới đã chỉnh sửa")
                        .param("isSelling", "true")
                        .param("isActive", "true")
                        .param("returnUrl", "/admin/products-manage?tab=ALL&page=0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products-manage?tab=ALL&page=0"))
                .andExpect(flash().attributeExists("successMessage"));

        Product updated = productRepository.findById(licensedProduct1.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("Bó Hoa Hồng Đỏ Đã Cập Nhật", updated.getName());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("290000"), updated.getPriceBuy());
        org.junit.jupiter.api.Assertions.assertEquals(10, updated.getSold()); // Số lượng đã bán được bảo toàn
    }

    /**
     * Kiểm thử thao tác Ngừng bán / Mở bán lại sản phẩm
     */
    @Test
    @WithMockUser(username = "admin_uc28", roles = {"ADMIN"})
    void testToggleSellingProduct() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin/products-manage/toggle-selling/" + licensedProduct1.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .param("returnUrl", "/admin/products-manage?page=0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products-manage?page=0"))
                .andExpect(flash().attributeExists("successMessage"));

        Product toggled = productRepository.findById(licensedProduct1.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertFalse(toggled.getIsSelling());
    }

    /**
     * Kiểm thử thao tác Xóa sản phẩm: Xóa mềm (isDeleted = true), bảo toàn entity và dữ liệu đơn hàng
     */
    @Test
    @WithMockUser(username = "admin_uc28", roles = {"ADMIN"})
    void testSoftDeleteProduct() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin/products-manage/delete/" + licensedProduct1.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .param("returnUrl", "/admin/products-manage?page=0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products-manage?page=0"))
                .andExpect(flash().attributeExists("successMessage"));

        Product softDeleted = productRepository.findById(licensedProduct1.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(softDeleted.getIsDeleted());
        org.junit.jupiter.api.Assertions.assertFalse(softDeleted.getIsSelling());
        org.junit.jupiter.api.Assertions.assertFalse(softDeleted.getIsActive());

        // Kiểm tra sản phẩm đã xóa không còn xuất hiện trong danh sách hiển thị
        mockMvc.perform(get("/admin/products-manage"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Bó Hoa Hồng Đỏ Cấp Phép UC28"))));
    }

    /**
     * Kiểm thử cột Tồn kho / Tổng tồn kho: Hiển thị đúng số lượng, đúng đơn vị (bó, lẵng...), nhãn Sắp hết / Hết hàng
     * và modal chi tiết kho từng chi nhánh.
     */
    @Test
    @WithMockUser(username = "admin_uc28", roles = {"ADMIN"})
    void testInventoryColumn_DisplaysStockQuantityWithCorrectUnitAndModalTrigger() throws Exception {
        mockMvc.perform(get("/admin/products-manage"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tổng tồn kho")))
                // Còn hàng: 24 bó
                .andExpect(content().string(containsString("24 bó")))
                // Sắp hết (<= 5): 3 lẵng · Sắp hết
                .andExpect(content().string(containsString("3 lẵng · Sắp hết")))
                // Hết hàng (<= 0): 0 bó · Hết hàng
                .andExpect(content().string(containsString("0 bó · Hết hàng")))
                // Modal chi tiết tồn kho chi nhánh
                .andExpect(content().string(containsString("id=\"inventoryModal" + licensedProduct1.getId() + "\"")))
                .andExpect(content().string(containsString("Chi nhánh Bến Thành UC28")))
                .andExpect(content().string(containsString("Tồn thực tế")))
                .andExpect(content().string(containsString("Đang giữ")))
                .andExpect(content().string(containsString("Có thể bán")));
    }

    /**
     * Kiểm thử bộ lọc Tồn kho (Tất cả / Còn hàng / Sắp hết / Hết hàng)
     */
    @Test
    @WithMockUser(username = "admin_uc28", roles = {"ADMIN"})
    void testInventoryFilter_ByStockStatus() throws Exception {
        // Lọc Sắp hết: Chỉ xuất hiện licensedProduct2 (3 lẵng)
        mockMvc.perform(get("/admin/products-manage")
                        .param("stockStatus", "LOW_STOCK"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Lẵng Hoa Lan Hồ Điệp Cấp Phép UC28")))
                .andExpect(content().string(not(containsString("Bó Hoa Hồng Đỏ Cấp Phép UC28"))))
                .andExpect(content().string(not(containsString("Hoa Ngoại Nhập Bị Cấm UC28"))));

        // Lọc Hết hàng: Chỉ xuất hiện unlicensedProduct1 (0 bó)
        mockMvc.perform(get("/admin/products-manage")
                        .param("stockStatus", "OUT_OF_STOCK"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hoa Ngoại Nhập Bị Cấm UC28")))
                .andExpect(content().string(not(containsString("Bó Hoa Hồng Đỏ Cấp Phép UC28"))))
                .andExpect(content().string(not(containsString("Lẵng Hoa Lan Hồ Điệp Cấp Phép UC28"))));

        // Lọc Còn hàng: Chỉ xuất hiện licensedProduct1 (24 bó)
        mockMvc.perform(get("/admin/products-manage")
                        .param("stockStatus", "IN_STOCK"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bó Hoa Hồng Đỏ Cấp Phép UC28")))
                .andExpect(content().string(not(containsString("Lẵng Hoa Lan Hồ Điệp Cấp Phép UC28"))))
                .andExpect(content().string(not(containsString("Hoa Ngoại Nhập Bị Cấm UC28"))));
    }

    /**
     * Kiểm thử khi Admin lọc theo một chi nhánh cụ thể: Cột đổi tiêu đề thành "Tồn kho"
     */
    @Test
    @WithMockUser(username = "admin_uc28", roles = {"ADMIN"})
    void testInventoryFilter_ByStoreId_ChangesColumnTitleToTonKho() throws Exception {
        mockMvc.perform(get("/admin/products-manage")
                        .param("storeId", store1.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("stockColumnTitle", "Tồn kho"))
                .andExpect(content().string(containsString("Tồn kho")))
                .andExpect(content().string(containsString("Chi nhánh Bến Thành UC28")));
    }
}
