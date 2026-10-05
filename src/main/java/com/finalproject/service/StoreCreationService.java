package com.finalproject.service;

import com.finalproject.entity.*;
import com.finalproject.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;

@Service @RequiredArgsConstructor
public class StoreCreationService {
    private final StoreAccessService access;
    private final StoreRepository stores;
    private final UserRepository users;
    private final CommissionRepository commissions;
    private final StoreLevelRepository levels;
    private final Clock clock;

    @Transactional
    public Store create(Store input, Long commissionId, String requestId) {
        var current = access.currentUser();
        if (!current.hasRole(com.finalproject.enums.Role.MANAGER)
                && !current.hasRole(com.finalproject.enums.Role.ADMIN)) throw new org.springframework.security.access.AccessDeniedException("Chức năng dành cho người bán.");
        var owner = users.findLockedById(current.getId()).orElseThrow();
        String key = requestId == null || requestId.isBlank() ? null : owner.getId() + ":" + java.util.UUID.fromString(requestId);
        if (key != null) {
            var previous = stores.findByCreationRequestKey(key);
            if (previous.isPresent()) return previous.get();
        }
        if (commissionId == null) throw new IllegalArgumentException("Vui lòng chọn chính sách hoa hồng.");
        var commission = commissions.findById(commissionId).filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Chính sách hoa hồng không hợp lệ hoặc đã bị xóa."));
        String name = input.getName() == null ? "" : input.getName().trim();
        String address = input.getAddress() == null ? "" : input.getAddress().trim();
        String phone = input.getPhone() == null ? "" : input.getPhone().trim().replaceAll("[\\s.-]", "");
        if (name.length() < 3 || name.length() > 120 || address.length() < 5 || address.length() > 200
                || !phone.matches("^(?:\\+84|84|0)(?:(?:3[2-9]|5[25689]|7[06-9]|8[1-9]|9[0-9])\\d{7}|2\\d{9})$")) {
            throw new IllegalArgumentException("Kiểm tra tên (3–120), địa chỉ (5–200) và số điện thoại Việt Nam hợp lệ.");
        }
        if (input.getBio() != null && input.getBio().length() > 500) throw new IllegalArgumentException("Bio tối đa 500 ký tự.");
        var level = levels.findByIsDeletedFalseOrderByMinPointAsc().stream().filter(l -> Integer.valueOf(0).equals(l.getMinPoint())).findFirst().orElse(null);
        return stores.save(Store.builder().name(name).address(address).phone(phone).bio(input.getBio())
                .owner(owner).commission(commission).storeLevel(level).rating(0.0)
                .isOpen(input.getIsOpen() == null || input.getIsOpen()).createdAt(LocalDateTime.now(clock)).creationRequestKey(key).build());
    }
}
