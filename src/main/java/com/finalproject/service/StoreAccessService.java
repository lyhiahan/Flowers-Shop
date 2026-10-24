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
        Store assigned = managedStore(user);
        return assigned == null ? java.util.List.of() : java.util.List.of(assigned);
    }

    /** One primary assignment; ambiguous legacy links never grant access to several shops. */
    public Store managedStore(User user) {
        if (!user.isStoreWorker() || user.hasRole(Role.ADMIN)
                || !Boolean.TRUE.equals(user.getIsActive()) || Boolean.TRUE.equals(user.getAccountDisabledByAdmin())
                || user.isRetiredDeliveryAccount()) return null;
        if (user.getStore() != null) {
            Store primary = user.getStore();
            var member = memberships.findByUserIdAndStoreId(user.getId(), primary.getId());
            boolean owner = primary.getOwner() != null && user.getId().equals(primary.getOwner().getId());
            return owner || member.isEmpty() || Boolean.TRUE.equals(member.get().getActive()) ? primary : null;
        }
        java.util.Map<Long, Store> legacy = new java.util.LinkedHashMap<>();
        stores.findByOwnerId(user.getId()).forEach(s -> legacy.put(s.getId(), s));
        memberships.findByUserIdAndActiveTrue(user.getId()).forEach(m -> legacy.put(m.getStore().getId(), m.getStore()));
        return legacy.size() == 1 ? legacy.values().iterator().next() : null;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public boolean managesStore(User user, Long storeId) {
        // Socket users may be detached; resolve lazy store links within this transaction.
        Store assigned = userRepository.findById(user.getId()).map(this::managedStore).orElse(null);
        return assigned != null && assigned.getId().equals(storeId);
    }

    public boolean hasStoreAffiliation(User user) {
        return user.getStore() != null || !stores.findByOwnerId(user.getId()).isEmpty()
                || memberships.existsByUserIdAndActiveTrue(user.getId());
    }

    public boolean canCreateStore() {
        User user = currentUser();
        return user.hasRole(Role.ADMIN) || user.hasRole(Role.MANAGER) && !hasStoreAffiliation(user);
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
        return store != null && managesStore(user, store.getId());
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
        requireManager(store);
        requireStore(store);
        if (!user.hasRole(Role.ADMIN) && (store.getOwner() == null || !user.getId().equals(store.getOwner().getId()))) {
            throw new AccessDeniedException("Chỉ quản lý phụ trách chi nhánh hoặc admin được thực hiện thao tác này.");
        }
    }

    public void requireStore(Store store) {
        User user = currentUser();
        if (user.hasRole(Role.ADMIN)) return;
        if (store == null || !user.isStoreWorker()) {
            throw new AccessDeniedException("Bạn không có quyền xử lý chi nhánh này.");
        }
        if (!canAccess(user, store)) throw new AccessDeniedException("Bạn không có quyền xử lý chi nhánh khác.");
    }
}
