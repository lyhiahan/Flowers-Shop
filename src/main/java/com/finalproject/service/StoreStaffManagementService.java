package com.finalproject.service;

import com.finalproject.entity.Store;
import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StoreStaffManagementService {
    private final StoreAccessService access;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final com.finalproject.repository.StoreMembershipRepository memberships;
    private final jakarta.persistence.EntityManager entityManager;

    @Transactional
    public int add(Store store, List<Long> ids, String username, String fullName, String email,
                   String phone, String password, String requestedRole) {
        access.requireOwner(store);
        if (!"STAFF".equalsIgnoreCase(requestedRole)) throw new IllegalArgumentException("Chỉ được thêm vai trò Nhân viên cửa hàng (STAFF).");
        Set<Long> selected = new TreeSet<>(ids == null ? List.of() : ids);
        String name = username == null ? "" : username.trim();
        User fresh = null;
        if (!name.isEmpty()) {
            var existing = users.findByUsername(name);
            if (existing.isPresent()) selected.add(existing.get().getId());
            else {
                if (!name.matches("[a-zA-Z0-9_.-]{3,50}")) {
                    throw new IllegalArgumentException("Tên đăng nhập không hợp lệ (yêu cầu từ 3 đến 50 ký tự, chỉ gồm chữ cái, chữ số, ., -, _).");
                }
                if (email == null || email.isBlank() || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") || email.length() > 100) {
                    throw new IllegalArgumentException("Email không đúng định dạng hoặc vượt quá 100 ký tự (VD: nhanvien@domain.com).");
                }
                if (users.existsByEmail(email.trim())) {
                    throw new IllegalArgumentException("Email '" + email.trim() + "' đã được sử dụng bởi một tài khoản khác trong hệ thống.");
                }
                if (password == null || password.length() < 8 || password.length() > 72) {
                    throw new IllegalArgumentException("Mật khẩu cho tài khoản mới phải từ 8 đến 72 ký tự.");
                }
                String display = fullName == null || fullName.isBlank() ? name : fullName.trim();
                if (display.length() > 100) throw new IllegalArgumentException("Họ và tên tối đa 100 ký tự.");
                String cleanPhone = (phone == null || phone.isBlank()) ? null : phone.trim().replaceAll("[\\s.-]", "");
                if (cleanPhone != null && !cleanPhone.matches("^(?:\\+84|84|0)(?:3[2-9]|5[25689]|7[06-9]|8[1-9]|9[0-9])\\d{7}$")) {
                    throw new IllegalArgumentException("Số điện thoại không hợp lệ (yêu cầu số điện thoại Việt Nam 10 chữ số, ví dụ: 0901234567 hoặc 0947475899).");
                }
                fresh = User.builder().username(name).email(email.trim()).fullName(display).phone(cleanPhone)
                        .password(encoder.encode(password)).role(Role.STAFF).isActive(true).isEmailActive(true).build();
            }
        }
        List<User> candidates = new ArrayList<>();
        for (Long id : selected) {
            User candidate = users.findLockedById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ứng viên."));
            entityManager.refresh(candidate, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            if (!Boolean.TRUE.equals(candidate.getIsActive()) || candidate.isRetiredDeliveryAccount()
                    || Boolean.TRUE.equals(candidate.getAccountDisabledByAdmin())
                    || candidate.getId().equals(access.currentUser().getId()) || candidate.hasRole(Role.ADMIN)
                    || !candidate.hasRole(Role.STAFF) || candidate.hasRole(Role.MANAGER) || access.hasStoreAffiliation(candidate)
                    || memberships.findByUserIdAndStoreId(candidate.getId(), store.getId()).filter(m -> Boolean.TRUE.equals(m.getActive())).isPresent()) {
                throw new IllegalArgumentException("Ứng viên không hợp lệ hoặc đã thuộc cửa hàng khác.");
            }
            candidates.add(candidate);
        }
        if (fresh != null) candidates.add(fresh);
        if (candidates.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn nhân viên cần thêm từ danh sách ứng viên hoặc điền thông tin để tạo mới.");
        for (User candidate : candidates) {
            candidate.setStore(store);
            users.save(candidate);
            var member = memberships.findByUserIdAndStoreId(candidate.getId(), store.getId()).orElseGet(() ->
                    com.finalproject.entity.StoreMembership.builder().user(candidate).store(store).build());
            member.setAccessRole("STAFF"); member.setActive(true); memberships.save(member);
        }
        return candidates.size();
    }

    @Transactional
    public void remove(Store store, Long id) {
        access.requireOwner(store);
        User staff = users.findLockedById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên."));
        if (store.getOwner() != null && staff.getId().equals(store.getOwner().getId())) {
            throw new IllegalArgumentException("Không thể xóa chủ cửa hàng.");
        }
        var member = memberships.findByUserIdAndStoreId(id, store.getId());
        boolean legacy = staff.getStore() != null && store.getId().equals(staff.getStore().getId());
        if (!legacy && member.filter(m -> Boolean.TRUE.equals(m.getActive())).isEmpty()) {
            throw new IllegalArgumentException("Nhân viên không thuộc cửa hàng này.");
        }
        if (legacy) staff.setStore(null);
        member.ifPresent(m -> { m.setActive(false); memberships.save(m); });
        users.save(staff);
    }
    @Transactional
    public void leave(Store store) {
        User user = access.currentUser(); access.requireStore(store);
        if (store.getOwner() != null && user.getId().equals(store.getOwner().getId())) throw new IllegalArgumentException("Quản lý phụ trách không thể tự rời chi nhánh.");
        var member = memberships.findByUserIdAndStoreId(user.getId(), store.getId()).orElseGet(() ->
                com.finalproject.entity.StoreMembership.builder().store(store).user(user).accessRole("STAFF").build());
        member.setActive(false); memberships.save(member);
        if (user.getStore() != null && store.getId().equals(user.getStore().getId())) { user.setStore(null); users.save(user); }
        access.clearSelection();
    }
}
