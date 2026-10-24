package com.finalproject.service;

import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountDeletionService {
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerAccountClosureService accountClosure;
    private final EntityManager entityManager;

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(Long id, String actor) {
        User user = userRepository.findLockedById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
        entityManager.refresh(user, LockModeType.PESSIMISTIC_WRITE);
        if (user.hasRole(Role.ADMIN) || "admin".equalsIgnoreCase(user.getUsername()))
            throw new IllegalArgumentException("Không thể xóa tài khoản Quản trị viên.");
        if (user.getUsername().equalsIgnoreCase(actor))
            throw new IllegalArgumentException("Bạn không thể tự xóa tài khoản đang đăng nhập.");
        if (!storeRepository.findByOwnerId(id).isEmpty())
            throw new IllegalArgumentException("Không thể xóa tài khoản đang phụ trách chi nhánh. Hãy phân công quản lý khác trước.");
        String username = user.getUsername();
        // Retain workers and referenced accounts to preserve business and conversation history.
        boolean retain = user.isStoreWorker()
                || !orderRepository.findByUserIdOrderByCreatedAtDesc(id).isEmpty()
                || orderRepository.existsByLegacyDeliveryContactId(id)
                || referenced("chat_conversations", "customer_id", id)
                || referenced("chat_messages", "sender_id", id)
                || referenced("store_memberships", "user_id", id)
                || referenced("jwt_sessions", "user_id", id)
                || referenced("role_requests", "reviewed_by_id", id);
        if (retain) {
            accountClosure.disable(id, true);
            return username;
        }

        // 4. Xóa giỏ hàng và các phụ thuộc
        cartRepository.findByUserId(id).ifPresent(c -> {
            if (c.getItems() != null && !c.getItems().isEmpty()) {
                cartItemRepository.deleteAll(c.getItems());
            }
            cartRepository.delete(c);
        });

        // Xóa các bảng phụ thuộc an toàn bằng native queries
        entityManager.createNativeQuery("DELETE FROM user_roles WHERE user_id = :uid").setParameter("uid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM user_addresses WHERE user_id = :uid").setParameter("uid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM product_wishlists WHERE user_id = :uid").setParameter("uid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM recently_viewed WHERE user_id = :uid").setParameter("uid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM notifications WHERE user_id = :uid").setParameter("uid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM role_requests WHERE user_id = :uid").setParameter("uid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM role_audit_logs WHERE user_id = :uid").setParameter("uid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM otp_verifications WHERE user_id = :uid").setParameter("uid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM user_follow_store WHERE user_id = :uid").setParameter("uid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM activity_logs WHERE user_id = :uid").setParameter("uid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM reviews WHERE user_id = :uid").setParameter("uid", id).executeUpdate();

        // Xóa người dùng khỏi database
        userRepository.delete(user);


        entityManager.flush();
        return username;
    }

    private boolean referenced(String table, String column, Long id) {
        return ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM " + table + " WHERE " + column + " = :uid")
                .setParameter("uid", id).getSingleResult()).longValue() > 0;
    }
}
