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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Kiểm thử Use Case UC_023: Quản lý ví điện tử của cửa hàng (Store Management – Ewallet)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StoreManagementEwalletUseCaseTests {

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

    private User shopOwner;
    private User staff;
    private Store store;
    private Transaction tx1;
    private Transaction tx2;

    @BeforeEach
    void setUp() {
        String encodedPass = passwordEncoder.encode("secret123");

        // Chủ cửa hàng
        shopOwner = userRepository.save(User.builder()
                .username("owner_uc23")
                .email("owner_uc23@example.com")
                .password(encodedPass)
                .fullName("Chủ Cửa Hàng UC23")
                .phone("0908889999")
                .role(Role.MANAGER)
                .isActive(true)
                .build());

        // Nhân viên / quản lý đối tác (không phải chủ sở hữu)
        staff = userRepository.save(User.builder()
                .username("staff_uc23")
                .email("staff_uc23@example.com")
                .password(encodedPass)
                .fullName("Nhân Viên UC23")
                .phone("0907778888")
                .role(Role.MANAGER)
                .isActive(true)
                .build());

        // Cửa hàng
        store = storeRepository.save(Store.builder()
                .name("Tiệm Hoa GD Coins UC23")
                .address("456 Nguyễn Huệ, Q1")
                .phone("0908889999")
                .owner(shopOwner)
                .isOpen(true)
                .status(true)
                .rating(5.0)
                .eWallet(BigDecimal.valueOf(1000000)) // 1.000.000 VNĐ
                .build());

        shopOwner.setStore(store);
        userRepository.save(shopOwner);

        staff.setStore(store);
        userRepository.save(staff);

        // Giao dịch mẫu
        tx1 = transactionRepository.save(Transaction.builder()
                .store(store)
                .amount(BigDecimal.valueOf(300000))
                .isUp(true)
                .type("ORDER_PAYMENT")
                .status("COMPLETED")
                .description("Thu nhập đơn hoa #101")
                .createdAt(LocalDateTime.now().minusHours(2))
                .build());

        tx2 = transactionRepository.save(Transaction.builder()
                .store(store)
                .amount(BigDecimal.valueOf(200000))
                .isUp(true)
                .type("ORDER_PAYMENT")
                .status("COMPLETED")
                .description("Thu nhập đơn hoa #102")
                .createdAt(LocalDateTime.now().minusHours(1))
                .build());
    }

    /**
     * Test Main Flow bước 6, 7, 9:
     * Vào trang GD Coins, hiển thị số dư, lịch sử giao dịch và nút Withdraw cho chủ cửa hàng
     */
    @Test
    @WithMockUser(username = "owner_uc23", roles = {"MANAGER"})
    void testViewWalletSuccessAsOwner() throws Exception {
        mockMvc.perform(get("/manager/wallet"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/wallet"))
                .andExpect(model().attribute("isOwner", true))
                .andExpect(model().attributeExists("transactions", "store"))
                .andExpect(content().string(containsString("sidebarGdCoins")))
                .andExpect(content().string(containsString("Thu nhập đơn hoa #101")))
                .andExpect(content().string(containsString("btnWithdraw")))
                .andExpect(content().string(containsString("withdrawModal")))
                .andExpect(content().string(containsString("confirmWithdrawModal")))
                .andExpect(content().string(containsString("btnConfirm")))
                .andExpect(content().string(containsString("btnCancel")));
    }

    /**
     * Test bước 9: Người quản lý KHÔNG phải chủ cửa hàng thì nút Withdraw không tồn tại
     */
    @Test
    @WithMockUser(username = "staff_uc23", roles = {"MANAGER"})
    void testViewWalletAsNonOwnerHidesWithdrawButton() throws Exception {
        mockMvc.perform(get("/manager/wallet"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/wallet"))
                .andExpect(model().attribute("isOwner", false))
                .andExpect(content().string(not(containsString("id=\"btnWithdraw\""))));
    }

    /**
     * Test Main Flow bước 8: Phân trang danh sách giao dịch
     */
    @Test
    @WithMockUser(username = "owner_uc23", roles = {"MANAGER"})
    void testPaginationOnTransactions() throws Exception {
        mockMvc.perform(get("/manager/wallet")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager/wallet"))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalPages", 2))
                .andExpect(content().string(containsString("btnNextPage")))
                .andExpect(content().string(containsString("btnPrevPage")));
    }

    /**
     * Test Main Flow bước 10-14: Yêu cầu rút tiền thành công khi nhập đúng mật khẩu
     */
    @Test
    @WithMockUser(username = "owner_uc23", roles = {"MANAGER"})
    void testWithdrawSuccessWithCorrectPassword() throws Exception {
        mockMvc.perform(post("/manager/wallet/withdraw")
                        .param("amount", "200000")
                        .param("password", "secret123")
                        .param("bankName", "Vietcombank")
                        .param("accountNumber", "1234567890")
                        .param("accountHolder", "CHỦ CỬA HÀNG UC23")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/wallet"))
                .andExpect(flash().attributeExists("successMessage"));

        Store updated = storeRepository.findById(store.getId()).orElseThrow();
        assertThat(updated.getEWallet()).isEqualByComparingTo("800000"); // 1.000.000 - 200.000
    }

    /**
     * Test bước 11 Exception: Nhập sai mật khẩu thì báo lỗi và không trừ tiền
     */
    @Test
    @WithMockUser(username = "owner_uc23", roles = {"MANAGER"})
    void testWithdrawFailsWithIncorrectPassword() throws Exception {
        mockMvc.perform(post("/manager/wallet/withdraw")
                        .param("amount", "200000")
                        .param("password", "wrong_password")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/wallet"))
                .andExpect(flash().attributeExists("errorMessage"));

        Store updated = storeRepository.findById(store.getId()).orElseThrow();
        assertThat(updated.getEWallet()).isEqualByComparingTo("1000000"); // Không bị trừ
    }
}
