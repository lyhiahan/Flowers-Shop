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

    @Transactional
    public int add(Store store, List<Long> ids, String username, String fullName, String email,
                   String phone, String password, String requestedRole) {
        access.requireOwner(store);
        boolean partner = "PARTNER".equalsIgnoreCase(requestedRole);
        if (!partner && !"STAFF".equalsIgnoreCase(requestedRole)) throw new IllegalArgumentException("Vai trò nhân viên không hợp lệ.");
        Set<Long> selected = new TreeSet<>(ids == null ? List.of() : ids);
        String name = username == null ? "" : username.trim();
        User fresh = null;
        if (!name.isEmpty()) {
            var existing = users.findByUsername(name);
            if (existing.isPresent()) selected.add(existing.get().getId());
            else {
                if (partner) throw new IllegalArgumentException("Đối tác cần có tài khoản người bán trên hệ thống.");
                if (!name.matches("[a-zA-Z0-9_.-]{3,50}") || password == null || password.length() < 8 || password.length() > 72
                        || email == null || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") || email.length() > 100
                        || users.existsByEmail(email.trim())) {
                    throw new IllegalArgumentException("Nhập username, email hợp lệ và mật khẩu 8–72 ký tự cho tài khoản mới.");
                }
                String display = fullName == null || fullName.isBlank() ? name : fullName.trim();
                if (display.length() > 100) throw new IllegalArgumentException("Họ tên tối đa 100 ký tự.");
                fresh = User.builder().username(name).email(email.trim()).fullName(display).phone(phone)
                        .password(encoder.encode(password)).role(Role.STAFF).isActive(true).isEmailActive(true).build();
            }
        }
        List<User> candidates = new ArrayList<>();
        for (Long id : selected) {
            User candidate = users.findLockedById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ứng viên."));
            if (!Boolean.TRUE.equals(candidate.getIsActive()) || candidate.isRetiredDeliveryAccount()
                    || Boolean.TRUE.equals(candidate.getAccountDisabledByAdmin())
                    || candidate.getId().equals(access.currentUser().getId()) || candidate.hasRole(Role.ADMIN)
                    || (partner ? !(candidate.hasRole(Role.VENDOR) || candidate.hasRole(Role.MANAGER))
                        : (candidate.getStore() != null || memberships.existsByUserIdAndActiveTrue(candidate.getId())
                            || (candidate.getRole() != Role.CUSTOMER && candidate.getRole() != Role.STAFF)
                            || candidate.hasRole(Role.VENDOR) || candidate.hasRole(Role.MANAGER)))
                    || memberships.findByUserIdAndStoreId(candidate.getId(), store.getId()).filter(m -> Boolean.TRUE.equals(m.getActive())).isPresent()) {
                throw new IllegalArgumentException("Ứng viên không hợp lệ hoặc đã thuộc cửa hàng khác.");
            }
            candidates.add(candidate);
        }
        if (fresh != null) candidates.add(fresh);
        if (candidates.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn nhân viên cần thêm.");
        for (User candidate : candidates) {
            if (!partner) {
                candidate.setStore(store);
                if (candidate.getRole() == Role.CUSTOMER) candidate.setRole(Role.STAFF);
                candidate.addRole(Role.STAFF);
            }
            users.save(candidate);
            var member = memberships.findByUserIdAndStoreId(candidate.getId(), store.getId()).orElseGet(() ->
                    com.finalproject.entity.StoreMembership.builder().user(candidate).store(store).build());
            member.setAccessRole(partner ? "PARTNER" : "STAFF"); member.setActive(true); memberships.save(member);
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
        if (store.getOwner() != null && user.getId().equals(store.getOwner().getId())) throw new IllegalArgumentException("Chủ cửa hàng không thể tự rời cửa hàng.");
        var member = memberships.findByUserIdAndStoreId(user.getId(), store.getId()).orElseGet(() ->
                com.finalproject.entity.StoreMembership.builder().store(store).user(user).accessRole("PARTNER").build());
        member.setActive(false); memberships.save(member);
        if (user.getStore() != null && store.getId().equals(user.getStore().getId())) { user.setStore(null); users.save(user); }
        access.clearSelection();
    }
}
