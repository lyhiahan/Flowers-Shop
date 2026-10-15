package com.finalproject.service;

import com.finalproject.entity.*;
import com.finalproject.enums.Role;
import com.finalproject.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DemoStaffSeeder {
    private final UserRepository userRepository;
    private final StoreMembershipRepository storeMembershipRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void seed(Store storeB, User partnerVendor) {
        // Đối tác quản lý nhượng quyền (Partner)
        if (storeMembershipRepository.findByUserIdAndStoreId(partnerVendor.getId(), storeB.getId()).isEmpty()) {
            storeMembershipRepository.save(StoreMembership.builder()
                    .user(partnerVendor)
                    .store(storeB)
                    .accessRole("PARTNER")
                    .active(true)
                    .joinedAt(LocalDateTime.now().minusMonths(3))
                    .build());
        }

        // Nhân viên 1: Nghệ nhân cắm hoa chính
        User staff1 = userRepository.findByUsername("nguyenmailan").or(() -> userRepository.findByEmail("mailan.florist@rentalshop.vn")).orElseGet(() -> userRepository.save(User.builder()
                .username("nguyenmailan")
                .password(passwordEncoder.encode("123456"))
                .fullName("Nguyễn Thị Mai Lan")
                .email("mailan.florist@rentalshop.vn")
                .phone("0938112233")
                .role(Role.MANAGER)
                .store(storeB)
                .isActive(true)
                .isEmailActive(true)
                .createdDate(LocalDate.now().minusMonths(5))
                .build()));
        staff1.setStore(storeB);
        staff1.setRole(Role.MANAGER);
        staff1.addRole(Role.MANAGER);
        userRepository.save(staff1);
        if (storeMembershipRepository.findByUserIdAndStoreId(staff1.getId(), storeB.getId()).isEmpty()) {
            storeMembershipRepository.save(StoreMembership.builder()
                    .user(staff1)
                    .store(storeB)
                    .accessRole("PARTNER")
                    .active(true)
                    .joinedAt(LocalDateTime.now().minusMonths(5))
                    .build());
        }

        // Nhân viên 2: Tiếp nhận & Điều phối đơn hàng
        User staff2 = userRepository.findByUsername("tranbaophuc").or(() -> userRepository.findByEmail("baophuc.orders@rentalshop.vn")).orElseGet(() -> userRepository.save(User.builder()
                .username("tranbaophuc")
                .password(passwordEncoder.encode("123456"))
                .fullName("Trần Bảo Phúc")
                .email("baophuc.orders@rentalshop.vn")
                .phone("0938223344")
                .role(Role.MANAGER)
                .store(storeB)
                .isActive(true)
                .isEmailActive(true)
                .createdDate(LocalDate.now().minusMonths(3))
                .build()));
        staff2.setStore(storeB);
        staff2.setRole(Role.MANAGER);
        staff2.addRole(Role.MANAGER);
        userRepository.save(staff2);
        if (storeMembershipRepository.findByUserIdAndStoreId(staff2.getId(), storeB.getId()).isEmpty()) {
            storeMembershipRepository.save(StoreMembership.builder()
                    .user(staff2)
                    .store(storeB)
                    .accessRole("PARTNER")
                    .active(true)
                    .joinedAt(LocalDateTime.now().minusMonths(3))
                    .build());
        }

        // Nhân viên 3: Thủ kho hoa tươi & nguyên phụ liệu
        User staff3 = userRepository.findByUsername("lehoangkhoi").or(() -> userRepository.findByEmail("hoangkhoi.stock@rentalshop.vn")).orElseGet(() -> userRepository.save(User.builder()
                .username("lehoangkhoi")
                .password(passwordEncoder.encode("123456"))
                .fullName("Lê Hoàng Khôi")
                .email("hoangkhoi.stock@rentalshop.vn")
                .phone("0938334455")
                .role(Role.MANAGER)
                .store(storeB)
                .isActive(true)
                .isEmailActive(true)
                .createdDate(LocalDate.now().minusMonths(2))
                .build()));
        staff3.setStore(storeB);
        staff3.setRole(Role.MANAGER);
        staff3.addRole(Role.MANAGER);
        userRepository.save(staff3);
        if (storeMembershipRepository.findByUserIdAndStoreId(staff3.getId(), storeB.getId()).isEmpty()) {
            storeMembershipRepository.save(StoreMembership.builder()
                    .user(staff3)
                    .store(storeB)
                    .accessRole("PARTNER")
                    .active(true)
                    .joinedAt(LocalDateTime.now().minusMonths(2))
                    .build());
        }
    }

}
