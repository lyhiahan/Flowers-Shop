package com.finalproject.service;

import com.finalproject.entity.*;
import com.finalproject.enums.Role;
import com.finalproject.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DemoBranchSeeder {
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final UserAddressRepository userAddressRepository;
    private final PasswordEncoder passwordEncoder;

    public record Branches(Store storeA, Store storeB, Store storeC, User customer) {}

    @Transactional
    public Branches seed() {
        // 1. Tạo hoặc cập nhật 3 chi nhánh mẫu sang tiếng Việt thuần
        Store storeA = storeRepository.findFirstByNameOrderByIdAsc("Chi Nhánh Bến Thành (Quận 1)")
                .orElseGet(() -> storeRepository.findFirstByNameOrderByIdAsc("Bloom Flower - Chi Nhánh Bến Thành (Quận 1)")
                        .map(s -> {
                            s.setName("Chi Nhánh Bến Thành (Quận 1)");
                            return storeRepository.save(s);
                        })
                        .orElseGet(() -> storeRepository.save(Store.builder()
                                .name("Chi Nhánh Bến Thành (Quận 1)")
                                .address("123 Lê Lợi, Phường Bến Thành, Quận 1, TP.HCM")
                                .phone("0281111222")
                                .status(true)
                                .isOpen(true)
                                .rating(4.9)
                                .eWallet(new BigDecimal("15500000"))
                                .build())));

        Store storeB = storeRepository.findFirstByNameOrderByIdAsc("Chi Nhánh Phú Mỹ Hưng (Quận 7)")
                .orElseGet(() -> storeRepository.findFirstByNameOrderByIdAsc("Bloom Flower - Chi Nhánh Phú Mỹ Hưng (Quận 7)")
                        .map(s -> {
                            s.setName("Chi Nhánh Phú Mỹ Hưng (Quận 7)");
                            return storeRepository.save(s);
                        })
                        .orElseGet(() -> storeRepository.save(Store.builder()
                                .name("Chi Nhánh Phú Mỹ Hưng (Quận 7)")
                                .address("456 Nguyễn Thị Thập, Tân Hưng, Quận 7, TP.HCM")
                                .phone("0283333444")
                                .status(true)
                                .isOpen(true)
                                .rating(4.8)
                                .eWallet(new BigDecimal("8200000"))
                                .build())));

        Store storeC = storeRepository.findFirstByNameOrderByIdAsc("Chi Nhánh Làng Đại Học (TP. Thủ Đức)")
                .orElseGet(() -> storeRepository.findFirstByNameOrderByIdAsc("Bloom Flower - Chi Nhánh Làng Đại Học (TP. Thủ Đức)")
                        .map(s -> {
                            s.setName("Chi Nhánh Làng Đại Học (TP. Thủ Đức)");
                            return storeRepository.save(s);
                        })
                        .orElseGet(() -> storeRepository.save(Store.builder()
                                .name("Chi Nhánh Làng Đại Học (TP. Thủ Đức)")
                                .address("1 Võ Văn Ngân, Linh Chiểu, Thủ Đức, TP.HCM")
                                .phone("0285555666")
                                .status(true)
                                .isOpen(true)
                                .rating(4.7)
                                .eWallet(new BigDecimal("4300000"))
                                .build())));

        // 2. Tạo các tài khoản mẫu cho từng Role (mật khẩu mặc định: 123456)
        userRepository.findByUsername("admin").orElseGet(() -> userRepository.save(User.builder()
                .username("admin")
                .password(passwordEncoder.encode("123456"))
                .fullName("Quản Trị Viên Toàn Hệ Thống")
                .email("admin@rentalshop.vn")
                .role(Role.ADMIN)
                .isActive(true)
                .isEmailActive(true)
                .createdDate(LocalDate.now())
                .build()));

        User managerA = userRepository.findByUsername("manager_q1").orElseGet(() -> userRepository.save(User.builder()
                .username("manager_q1")
                .password(passwordEncoder.encode("123456"))
                .fullName("Nguyễn Quản Lý Q1")
                .email("manager.q1@rentalshop.vn")
                .phone("0912345678")
                .role(Role.MANAGER)
                .store(storeA)
                .isActive(true)
                .isEmailActive(true)
                .createdDate(LocalDate.now())
                .build()));

        User vendor = userRepository.findByUsername("vendor_flower").orElseGet(() -> userRepository.save(User.builder()
                .username("vendor_flower")
                .password(passwordEncoder.encode("123456"))
                .fullName("Trần Đối Tác Vườn Hoa Đà Lạt")
                .email("vendor@rentalshop.vn")
                .phone("0988776655")
                .role(Role.MANAGER)
                .store(storeB)
                .isActive(true)
                .isEmailActive(true)
                .createdDate(LocalDate.now())
                .build()));

        // Demo fixtures must identify actual shop owners for owner-only staff and wallet actions.
        if (storeA.getOwner() == null) { storeA.setOwner(managerA); storeRepository.save(storeA); }
        if (storeB.getOwner() == null) { storeB.setOwner(vendor); storeRepository.save(storeB); }

        User customer = userRepository.findByUsername("khachhang").orElseGet(() -> userRepository.save(User.builder()
                .username("khachhang")
                .password(passwordEncoder.encode("123456"))
                .fullName("Lê Khách Hàng")
                .email("khachhang@gmail.com")
                .phone("0909999888")
                .address("789 Hai Bà Trưng, Phường 8, Quận 3, TP.HCM")
                .role(Role.USER)
                .isActive(true)
                .isEmailActive(true)
                .eWallet(new BigDecimal("2500000"))
                .createdDate(LocalDate.now())
                .build()));

        // Sổ địa chỉ cho khách hàng
        if (userAddressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(customer.getId()).isEmpty()) {
            userAddressRepository.save(UserAddress.builder()
                    .user(customer)
                    .recipientName("Lê Khách Hàng")
                    .phone("0909999888")
                    .addressDetail("789 Hai Bà Trưng, Phường 8, Quận 3, TP.HCM")
                    .isDefault(true)
                    .build());
            userAddressRepository.save(UserAddress.builder()
                    .user(customer)
                    .recipientName("Lê Khách Hàng (Văn phòng)")
                    .phone("0909999888")
                    .addressDetail("Toà nhà Bitexco, số 2 Hải Triều, Phường Bến Nghé, Quận 1, TP.HCM")
                    .isDefault(false)
                    .build());
        }

        return new Branches(storeA, storeB, storeC, customer);
    }
}
