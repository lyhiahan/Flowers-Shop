package com.finalproject.config;

import com.finalproject.entity.*;
import com.finalproject.enums.Role;
import com.finalproject.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final StoreInventoryRepository storeInventoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (storeRepository.count() > 0) {
            return; // Đã có dữ liệu, không tạo lại
        }

        // 1. Tạo 3 chi nhánh mẫu (có tọa độ Lat/Long tại TP.HCM)
        Store storeA = Store.builder()
                .name("Chi nhánh Quận 1 (Cơ sở chính)")
                .address("123 Lê Lợi, Phường Bến Thành, Quận 1, TP.HCM")
                .phone("0281111222")
                .status(true)
                .latitude(10.7725)
                .longitude(106.6980)
                .build();

        Store storeB = Store.builder()
                .name("Chi nhánh Quận 7")
                .address("456 Nguyễn Thị Thập, Tân Hưng, Quận 7, TP.HCM")
                .phone("0283333444")
                .status(true)
                .latitude(10.7380)
                .longitude(106.7130)
                .build();

        Store storeC = Store.builder()
                .name("Chi nhánh TP. Thủ Đức")
                .address("1 Võ Văn Ngân, Linh Chiểu, Thủ Đức, TP.HCM")
                .phone("0285555666")
                .status(true)
                .latitude(10.8505)
                .longitude(106.7719)
                .build();

        storeRepository.save(storeA);
        storeRepository.save(storeB);
        storeRepository.save(storeC);

        // 2. Tạo các tài khoản mẫu cho từng Role (mật khẩu mặc định: 123456)
        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("123456"))
                .fullName("Quản Trị Viên Toàn Hệ Thống")
                .email("admin@rentalshop.vn")
                .role(Role.ADMIN)
                .isActive(true)
                .createdDate(LocalDate.now())
                .build();

        User managerA = User.builder()
                .username("manager_q1")
                .password(passwordEncoder.encode("123456"))
                .fullName("Nguyễn Quản Lý Q1")
                .email("manager.q1@rentalshop.vn")
                .role(Role.MANAGER)
                .store(storeA)
                .isActive(true)
                .createdDate(LocalDate.now())
                .build();

        User staffA = User.builder()
                .username("staff_q1")
                .password(passwordEncoder.encode("123456"))
                .fullName("Trần Nhân Viên Q1")
                .email("staff.q1@rentalshop.vn")
                .role(Role.STAFF)
                .store(storeA)
                .isActive(true)
                .createdDate(LocalDate.now())
                .build();

        User staffB = User.builder()
                .username("staff_q7")
                .password(passwordEncoder.encode("123456"))
                .fullName("Lê Nhân Viên Q7")
                .email("staff.q7@rentalshop.vn")
                .role(Role.STAFF)
                .store(storeB)
                .isActive(true)
                .createdDate(LocalDate.now())
                .build();

        User customer = User.builder()
                .username("khachhang")
                .password(passwordEncoder.encode("123456"))
                .fullName("Lê Khách Hàng")
                .email("khachhang@gmail.com")
                .phone("0909999888")
                .address("TP. Hồ Chí Minh")
                .role(Role.CUSTOMER)
                .isActive(true)
                .createdDate(LocalDate.now())
                .build();

        userRepository.save(admin);
        userRepository.save(managerA);
        userRepository.save(staffA);
        userRepository.save(staffB);
        userRepository.save(customer);

        // 3. Tạo danh mục sản phẩm mẫu
        Category catCamping = Category.builder()
                .name("Đồ Dã Ngoại & Cắm Trại")
                .description("Lều, túi ngủ, đèn pin dã ngoại cao cấp")
                .build();
        categoryRepository.save(catCamping);

        // 4. Tạo sản phẩm mẫu
        Product tent = Product.builder()
                .category(catCamping)
                .name("Lều Cắm Trại Chống Nước 4 Người Vintage")
                .description("Lều vải oxford hai lớp chống thấm tuyệt đối, dễ dàng tháo lắp trong 5 phút.")
                .importPrice(new BigDecimal("1000000"))
                .priceBuy(new BigDecimal("1500000"))
                .priceRentPerDay(new BigDecimal("80000"))
                .depositFee(new BigDecimal("500000"))
                .build();
        productRepository.save(tent);

        // 5. Cấp phát tồn kho mẫu
        // Chi nhánh Q1 còn 5 cái, Chi nhánh Q7 còn 2 cái, Chi nhánh Thủ Đức tạm hết (0 cái)
        storeInventoryRepository.save(StoreInventory.builder().store(storeA).product(tent).quantity(5).build());
        storeInventoryRepository.save(StoreInventory.builder().store(storeB).product(tent).quantity(2).build());
        storeInventoryRepository.save(StoreInventory.builder().store(storeC).product(tent).quantity(0).build());
    }
}