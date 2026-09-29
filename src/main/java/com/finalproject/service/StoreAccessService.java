package com.finalproject.service;

import com.finalproject.entity.Store;
import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StoreAccessService {
    private final UserRepository userRepository;

    public User currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Vui lòng đăng nhập.");
        }
        return userRepository.findByUsername(authentication.getName())
                .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                .orElseThrow(() -> new AccessDeniedException("Tài khoản không hợp lệ."));
    }

    public Store assignedStore() {
        User user = currentUser();
        if (user.getStore() == null) {
            throw new AccessDeniedException("Tài khoản chưa được phân công chi nhánh.");
        }
        requireStore(user.getStore());
        return user.getStore();
    }

    public void requireManager(Store store) {
        User user = currentUser();
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.MANAGER && user.getRole() != Role.VENDOR) {
            throw new AccessDeniedException("Chức năng chỉ dành cho quản lý.");
        }
        requireStore(store);
    }

    public void requireStore(Store store) {
        User user = currentUser();
        if (user.getRole() == Role.ADMIN) return;
        if (store == null || (user.getRole() != Role.MANAGER && user.getRole() != Role.VENDOR && user.getRole() != Role.STAFF)) {
            throw new AccessDeniedException("Bạn không có quyền xử lý chi nhánh này.");
        }
        boolean assigned = user.getStore() != null && store.getId().equals(user.getStore().getId());
        boolean owner = store.getOwner() != null && user.getId().equals(store.getOwner().getId());
        if (!assigned && !owner) throw new AccessDeniedException("Bạn không có quyền xử lý chi nhánh khác.");
    }
}
