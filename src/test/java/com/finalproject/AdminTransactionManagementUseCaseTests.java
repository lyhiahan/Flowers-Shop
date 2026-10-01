package com.finalproject;

import com.finalproject.entity.Store;
import com.finalproject.entity.Transaction;
import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.repository.StoreRepository;
import com.finalproject.repository.TransactionRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Kiểm thử toàn diện Use Case UC_036: Quản lý giao dịch trong hệ thống (System management – Transaction)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.transaction.annotation.Transactional
class AdminTransactionManagementUseCaseTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User customer;
    private Store store;
    private Transaction tx1;
    private Transaction tx2;

    @BeforeEach
    void setUp() {
        String encodedPass = passwordEncoder.encode("secret123");

        admin = userRepository.save(User.builder()
                .username("admin_uc36")
                .email("admin_uc36@example.com")
                .password(encodedPass)
                .fullName("Quản Trị Viên UC36")
                .role(Role.ADMIN)
                .build());

        customer = userRepository.save(User.builder()
                .username("customer_uc36")
                .email("customer_uc36@example.com")
                .password(encodedPass)
                .fullName("Khách Hàng UC36")
                .role(Role.CUSTOMER)
                .build());

        store = storeRepository.save(Store.builder()
                .name("Chi Nhánh Hoa Sài Gòn UC36")
                .bio("Tiệm hoa tươi uy tín chất lượng")
                .address("123 Lê Lợi, Quận 1, TP.HCM")
                .isOpen(true)
                .status(true)
                .owner(admin)
                .build());

        tx1 = transactionRepository.save(Transaction.builder()
                .store(store)
                .user(admin)
                .isUp(true)
                .amount(new BigDecimal("500000"))
                .type("ORDER_PAYOUT")
                .description("Thu tiền đơn hàng thành công #101")
                .status("COMPLETED")
                .createdAt(LocalDateTime.now().minusHours(2))
                .build());

        tx2 = transactionRepository.save(Transaction.builder()
                .store(store)
                .user(admin)
                .isUp(false)
                .amount(new BigDecimal("200000"))
                .type("WITHDRAWAL")
                .description("Yêu cầu rút tiền về tài khoản ngân hàng")
                .status("PENDING")
                .createdAt(LocalDateTime.now().minusHours(1))
                .build());
    }

    /**
     * Pre-condition: Chỉ người dùng có quyền Admin mới được phép truy cập
     */
    @Test
    @WithMockUser(username = "customer_uc36", roles = {"CUSTOMER"})
    void testPreCondition_CustomerDeniedAccessToAdminTransactions() throws Exception {
        mockMvc.perform(get("/admin/transactions"))
                .andExpect(status().isForbidden());
    }

    /**
     * Main Flow Bước 1, 2 & 3:
     * 1. Vào trang Dashboard của Admin
     * 2. Chọn Transaction trên thanh sidebar
     * 3. Chuyển đến trang quản lý giao dịch, hiển thị bảng danh sách các giao dịch trong hệ thống
     */
    @Test
    @WithMockUser(username = "admin_uc36", roles = {"ADMIN"})
    void testStep1To3_NavigateFromDashboardToTransactions() throws Exception {
        // Bước 1 & 2: Vào trang Dashboard của Admin, kiểm tra thanh sidebar có Transaction link
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"sidebarTransactionsLink\"")))
                .andExpect(content().string(containsString("href=\"/admin/transactions\"")));

        // Bước 3: Chuyển đến trang quản lý giao dịch, hiển thị bảng danh sách các giao dịch trong hệ thống
        mockMvc.perform(get("/admin/transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/transactions"))
                .andExpect(model().attributeExists("transactions"))
                .andExpect(content().string(containsString("id=\"transactionsTable\"")))
                .andExpect(content().string(containsString("Thu tiền đơn hàng thành công #101")))
                .andExpect(content().string(containsString("Yêu cầu rút tiền về tài khoản ngân hàng")));
    }

    /**
     * Main Flow Bước 4:
     * 4. Chọn Next (hoặc Previous, hoặc trang bất kỳ) để xem trang tiếp theo (hoặc trang trước đó, hoặc trang tương ứng)
     */
    @Test
    @WithMockUser(username = "admin_uc36", roles = {"ADMIN"})
    void testStep4_PaginationNextPreviousAndSpecificPage() throws Exception {
        // Tạo thêm các giao dịch để phân trang
        for (int i = 1; i <= 15; i++) {
            transactionRepository.save(Transaction.builder()
                    .store(store)
                    .user(admin)
                    .isUp(i % 2 == 0)
                    .amount(new BigDecimal(50000 + i * 10000))
                    .type(i % 2 == 0 ? "ORDER_PAYOUT" : "COMMISSION_FEE")
                    .description("Giao dịch hệ thống tự động kiểm thử #" + i)
                    .status("COMPLETED")
                    .createdAt(LocalDateTime.now().minusMinutes(i * 10))
                    .build());
        }

        // Trang 1 (page = 0, size = 5)
        mockMvc.perform(get("/admin/transactions?page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"transactionsTable\"")))
                .andExpect(content().string(containsString("id=\"transactionsPagination\"")))
                .andExpect(content().string(containsString("id=\"btnPrevPage\"")))
                .andExpect(content().string(containsString("id=\"btnNextPage\"")))
                .andExpect(content().string(containsString("id=\"pageBtn_1\"")))
                .andExpect(content().string(containsString("id=\"pageBtn_2\"")))
                .andExpect(model().attribute("currentPage", 0));

        // Chọn Next / Trang 2 (page = 1, size = 5)
        mockMvc.perform(get("/admin/transactions?page=1&size=5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"transactionsTable\"")))
                .andExpect(model().attribute("currentPage", 1));

        // Chọn trang cụ thể (page = 2, size = 5)
        mockMvc.perform(get("/admin/transactions?page=2&size=5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"transactionsTable\"")))
                .andExpect(model().attribute("currentPage", 2));
    }

    /**
     * Search / Filter: Lọc danh sách giao dịch theo từ khóa nội dung
     */
    @Test
    @WithMockUser(username = "admin_uc36", roles = {"ADMIN"})
    void testTransactionSearchByKeyword() throws Exception {
        mockMvc.perform(get("/admin/transactions?keyword=Thu tiền đơn hàng"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Thu tiền đơn hàng thành công #101")));
    }

    /**
     * Post-conditions:
     * - Nếu thành công, hiển thị bảng danh sách các giao dịch trong hệ thống
     * - Nếu thất bại, thông báo lỗi
     */
    @Test
    @WithMockUser(username = "admin_uc36", roles = {"ADMIN"})
    void testPostConditions_SuccessTableAndErrorMessage() throws Exception {
        // Trường hợp thành công: hiển thị bảng danh sách giao dịch
        mockMvc.perform(get("/admin/transactions"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"transactionsTable\"")));

        // Trường hợp lỗi: hiển thị thông báo lỗi
        mockMvc.perform(get("/admin/transactions")
                        .flashAttr("errorMessage", "Có lỗi xảy ra khi xử lý giao dịch!"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"dataLoadAlert\"")))
                .andExpect(content().string(containsString("Có lỗi xảy ra khi xử lý giao dịch!")));
    }
}
