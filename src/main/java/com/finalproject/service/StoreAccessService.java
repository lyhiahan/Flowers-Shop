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
    private final com.finalproject.repository.StoreRepository stores;
    private final com.finalproject.repository.StoreMembershipRepository memberships;
    private jakarta.servlet.http.HttpServletRequest request() {
        var attributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        return attributes instanceof org.springframework.web.context.request.ServletRequestAttributes servlet ? servlet.getRequest() : null;
    }

    public User currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Vui lòng đăng nhập.");
        }
        return userRepository.findByUsername(authentication.getName())
                .filter(user -> Boolean.TRUE.equals(user.getIsActive()) && !user.isRetiredDeliveryAccount()
                        && !Boolean.TRUE.equals(user.getAccountDisabledByAdmin()))
                .orElseThrow(() -> new AccessDeniedException("Tài khoản không hợp lệ."));
    }

    public Store assignedStore() {
        Store store = selectedStore();
        if (store == null) {
            throw new AccessDeniedException("Tài khoản chưa được phân công chi nhánh.");
        }
        requireStore(store);
        return store;
    }

    public java.util.List<Store> accessibleStores() {
        User user = currentUser();
        if (user.hasRole(Role.ADMIN)) return stores.findAll();
        java.util.Map<Long, Store> result = new java.util.LinkedHashMap<>();
        stores.findByOwnerId(user.getId()).forEach(s -> result.put(s.getId(), s));
        memberships.findByUserIdAndActiveTrue(user.getId()).forEach(m -> result.put(m.getStore().getId(), m.getStore()));
        if (user.getStore() != null && canAccess(user, user.getStore())) result.put(user.getStore().getId(), user.getStore());
        return new java.util.ArrayList<>(result.values());
    }

    public Store selectedStore() {
        User user = currentUser();
        var request = request();
        var session = request == null ? null : request.getSession(false);
        if (session != null && session.getAttribute("activeStoreId") instanceof Long id) {
            var selected = stores.findById(id).filter(s -> canAccess(user, s));
            if (selected.isPresent()) return selected.get();
            session.removeAttribute("activeStoreId");
        }
        if (user.getStore() != null && canAccess(user, user.getStore())) return user.getStore();
        return accessibleStores().stream().findFirst().orElse(null);
    }

    public void select(Store store) {
        requireStore(store);
        var request = request();
        if (request != null) request.getSession().setAttribute("activeStoreId", store.getId());
    }

    public void clearSelection() {
        var request = request();
        if (request != null && request.getSession(false) != null) request.getSession(false).removeAttribute("activeStoreId");
    }

    private boolean canAccess(User user, Store store) {
        if (user.hasRole(Role.ADMIN)) return true;
        if (store.getOwner() != null && user.getId().equals(store.getOwner().getId())) return true;
        var member = memberships.findByUserIdAndStoreId(user.getId(), store.getId());
        if (member.isPresent()) return Boolean.TRUE.equals(member.get().getActive());
        // Compatibility for assignments created before the membership migration.
        return user.getStore() != null && store.getId().equals(user.getStore().getId());
    }

    public void requireManager(Store store) {
        User user = currentUser();
        if (!user.hasRole(Role.ADMIN) && !user.hasRole(Role.MANAGER)) {
            throw new AccessDeniedException("Chức năng chỉ dành cho quản lý.");
        }
        requireStore(store);
        if (!user.hasRole(Role.ADMIN) && (store.getOwner() == null || !user.getId().equals(store.getOwner().getId()))) {
            var member = memberships.findByUserIdAndStoreId(user.getId(), store.getId());
            if (member.isPresent() && !"PARTNER".equals(member.get().getAccessRole())) {
                throw new AccessDeniedException("Nhân viên không có quyền quản lý cửa hàng.");
            }
        }
    }

    public void requireOwner(Store store) {
        User user = currentUser();
        requireStore(store);
        if (store.getOwner() == null || !user.getId().equals(store.getOwner().getId())) {
            throw new AccessDeniedException("Chỉ chủ cửa hàng được thực hiện thao tác này.");
        }
    }

    public void requireStore(Store store) {
        User user = currentUser();
        if (user.hasRole(Role.ADMIN)) return;
        if (store == null || !user.hasRole(Role.MANAGER)) {
            throw new AccessDeniedException("Bạn không có quyền xử lý chi nhánh này.");
        }
        if (!canAccess(user, store)) throw new AccessDeniedException("Bạn không có quyền xử lý chi nhánh khác.");
    }
}
