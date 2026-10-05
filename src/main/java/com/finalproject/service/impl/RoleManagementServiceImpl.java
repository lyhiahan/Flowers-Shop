package com.finalproject.service.impl;

import com.finalproject.entity.RoleAuditLog;
import com.finalproject.entity.RoleRequest;
import com.finalproject.entity.Store;
import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.enums.RoleRequestStatus;
import com.finalproject.repository.RoleAuditLogRepository;
import com.finalproject.repository.RoleRequestRepository;
import com.finalproject.repository.StoreRepository;
import com.finalproject.repository.UserRepository;
import com.finalproject.service.RoleManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleManagementServiceImpl implements RoleManagementService {

    private final RoleRequestRepository roleRequestRepository;
    private final RoleAuditLogRepository roleAuditLogRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;

    @Override
    @Transactional
    public RoleRequest submitRoleRequest(Long userId,
                                         Role targetRole,
                                         String phone,
                                         Long targetStoreId,
                                         String shopName,
                                         String shopAddress,
                                         String contactInfo,
                                         String experienceNote) {
        User user = userRepository.findLockedById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin người dùng."));

        if (!Boolean.TRUE.equals(user.getIsActive()) || Boolean.TRUE.equals(user.getAccountDisabledByAdmin()) || user.isRetiredDeliveryAccount()) {
            throw new IllegalArgumentException("Tài khoản của bạn đang bị khóa, không thể gửi yêu cầu vai trò.");
        }

        if (targetRole != Role.MANAGER) {
            throw new IllegalArgumentException("Chỉ được gửi yêu cầu trở thành quản lý / chủ cửa hàng.");
        }

        if (user.hasRole(targetRole)) {
            throw new IllegalArgumentException("Bạn hiện tại đã sở hữu vai trò này rồi!");
        }

        boolean hasPending = roleRequestRepository.existsByUserIdAndTargetRoleAndStatus(
                userId, targetRole, RoleRequestStatus.PENDING
        );
        if (hasPending) {
            throw new IllegalArgumentException("Bạn đã có một yêu cầu cho vai trò này đang chờ duyệt. Vui lòng chờ phản hồi từ quản trị viên!");
        }

        Store targetStore = null;
        validateLength(phone, 20, "Số điện thoại");
        validateLength(shopName, 255, "Tên cửa hàng");
        validateLength(shopAddress, 500, "Địa chỉ cửa hàng");
        validateLength(contactInfo, 255, "Thông tin liên hệ");
        validateLength(experienceNote, 1000, "Kinh nghiệm");
        if (targetStoreId != null) {
            targetStore = storeRepository.findById(targetStoreId).filter(s -> Boolean.TRUE.equals(s.getStatus())).orElseThrow(() -> new IllegalArgumentException("Chi nhánh đăng ký không còn hoạt động."));
        } else if (shopName == null || shopName.isBlank() || shopAddress == null || shopAddress.isBlank()
                || contactInfo == null || contactInfo.isBlank()) {
            throw new IllegalArgumentException("Chọn chi nhánh hoặc nhập đủ tên, địa chỉ và thông tin liên hệ cửa hàng mới.");
        }

        RoleRequest req = RoleRequest.builder()
                .user(user)
                .targetRole(targetRole)
                .status(RoleRequestStatus.PENDING)
                .phone(phone != null ? phone.trim() : user.getPhone())
                .targetStore(targetStore)
                .shopName(shopName != null ? shopName.trim() : null)
                .shopAddress(shopAddress != null ? shopAddress.trim() : null)
                .contactInfo(contactInfo != null ? contactInfo.trim() : null)
                .experienceNote(experienceNote != null ? experienceNote.trim() : null)
                .createdAt(LocalDateTime.now())
                .build();

        return roleRequestRepository.save(req);
    }

    @Override
    @Transactional
    public void cancelRoleRequest(Long requestId, Long userId) {
        RoleRequest req = roleRequestRepository.findLockedById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu vai trò #" + requestId));

        if (!req.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Bạn không có quyền rút yêu cầu này.");
        }

        if (req.getStatus() != RoleRequestStatus.PENDING) {
            throw new IllegalArgumentException("Chỉ có thể rút yêu cầu khi đang ở trạng thái 'Chờ duyệt'.");
        }

        req.setStatus(RoleRequestStatus.CANCELLED);
        roleRequestRepository.save(req);
        log.info("Người dùng #{} đã rút yêu cầu vai trò #{}", userId, requestId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleRequest> getUserRoleRequests(Long userId) {
        return roleRequestRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleRequest> getAllRoleRequests(RoleRequestStatus status) {
        if (status == null) {
            return roleRequestRepository.findAllByOrderByCreatedAtDesc();
        }
        return roleRequestRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Override
    @Transactional
    public RoleRequest approveRoleRequest(Long requestId, Long adminId, Long assignedStoreId) {
        RoleRequest req = roleRequestRepository.findLockedById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu #" + requestId));

        if (req.isRetiredRoleRequest()) {
            throw new IllegalArgumentException("Vai trò này đã được ngừng sử dụng, không thể cấp lại.");
        }

        if (req.getStatus() != RoleRequestStatus.PENDING) {
            throw new IllegalArgumentException("Yêu cầu này không ở trạng thái 'Chờ duyệt'!");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Quản trị viên xét duyệt không tồn tại."));
        requireAdmin(admin);
        User user = req.getUser();
        if (!Boolean.TRUE.equals(user.getIsActive()) || Boolean.TRUE.equals(user.getAccountDisabledByAdmin()) || user.isRetiredDeliveryAccount())
            throw new IllegalArgumentException("Không thể cấp quyền cho tài khoản bị khóa.");

        Store storeToAssign = null;
        if (req.getTargetRole() == Role.MANAGER) {
            Long storeId = assignedStoreId != null ? assignedStoreId : (req.getTargetStore() != null ? req.getTargetStore().getId() : null);
            if (storeId == null && (req.getShopName() == null || req.getShopName().isBlank()
                    || req.getShopAddress() == null || req.getShopAddress().isBlank()
                    || req.getContactInfo() == null || req.getContactInfo().isBlank())) {
                throw new IllegalArgumentException("Khi duyệt vai trò Quản lý chi nhánh, quản trị viên bắt buộc phải chỉ định chi nhánh được quản lý!");
            }
            if (storeId != null) {
            storeToAssign = storeRepository.findById(storeId)
                    .orElseThrow(() -> new IllegalArgumentException("Chi nhánh phân công không tồn tại."));
            user.setStore(storeToAssign);
            req.setAssignedStore(storeToAssign);
            }
        }

        user.addRole(req.getTargetRole());
        user.setRole(req.getTargetRole());
        userRepository.save(user);

        req.setStatus(RoleRequestStatus.APPROVED);
        req.setReviewedBy(admin);
        req.setReviewedAt(LocalDateTime.now());
        RoleRequest savedReq = roleRequestRepository.save(req);

        // Ghi nhật ký phân quyền
        RoleAuditLog auditLog = RoleAuditLog.builder()
                .user(user)
                .action("GRANT")
                .role(req.getTargetRole())
                .store(storeToAssign)
                .reason("Phê duyệt yêu cầu vai trò #" + req.getId() + (storeToAssign != null ? " (Phân công chi nhánh: " + storeToAssign.getName() + ")" : ""))
                .performedBy(admin)
                .createdAt(LocalDateTime.now())
                .build();
        roleAuditLogRepository.save(auditLog);

        log.info("Admin {} đã duyệt vai trò {} cho người dùng {}", admin.getUsername(), req.getTargetRole(), user.getUsername());
        return savedReq;
    }

    @Override
    @Transactional
    public RoleRequest rejectRoleRequest(Long requestId, Long adminId, String reason) {
        RoleRequest req = roleRequestRepository.findLockedById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu #" + requestId));

        if (req.getStatus() != RoleRequestStatus.PENDING) {
            throw new IllegalArgumentException("Yêu cầu này không ở trạng thái 'Chờ duyệt'!");
        }

        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do từ chối yêu cầu vai trò!");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Quản trị viên xét duyệt không tồn tại."));
        requireAdmin(admin);

        req.setStatus(RoleRequestStatus.REJECTED);
        req.setRejectionReason(reason.trim());
        req.setReviewedBy(admin);
        req.setReviewedAt(LocalDateTime.now());
        RoleRequest savedReq = roleRequestRepository.save(req);

        // Người dùng vẫn giữ nguyên tài khoản khách hàng bình thường
        log.info("Admin {} đã từ chối yêu cầu vai trò #{} của người dùng {}. Lý do: {}",
                admin.getUsername(), requestId, req.getUser().getUsername(), reason);

        return savedReq;
    }

    @Override
    @Transactional
    public void directlyAssignRole(Long userId, Role role, Long storeId, String reason, Long adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản quản trị viên."));
        requireAdmin(admin);

        if (role == Role.ADMIN && !"admin".equalsIgnoreCase(admin.getUsername())) {
            throw new IllegalArgumentException("Chỉ Quản trị viên tối cao mới có quyền cấp vai trò Admin.");
        }

        Store store = null;
        if (role == Role.MANAGER && storeId != null) {
            store = storeRepository.findById(storeId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh được phân công."));
            user.setStore(store);
        }

        user.addRole(role);
        user.setRole(role);
        userRepository.save(user);

        RoleAuditLog logEntry = RoleAuditLog.builder()
                .user(user)
                .action("GRANT")
                .role(role)
                .store(store)
                .reason(reason != null && !reason.trim().isEmpty() ? reason.trim() : "Quản trị viên gán vai trò trực tiếp")
                .performedBy(admin)
                .createdAt(LocalDateTime.now())
                .build();
        roleAuditLogRepository.save(logEntry);

        log.info("Admin {} đã gán trực tiếp vai trò {} cho user {}", admin.getUsername(), role, user.getUsername());
    }

    @Override
    @Transactional
    public void directlyRevokeRole(Long userId, Role role, String reason, Long adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản quản trị viên."));
        requireAdmin(admin);

        if ("admin".equalsIgnoreCase(user.getUsername()) && role == Role.ADMIN) {
            throw new IllegalArgumentException("Không thể thu hồi vai trò của Quản trị viên tối cao!");
        }

        if (role == Role.USER) {
            throw new IllegalArgumentException("Không thể thu hồi vai trò Khách hàng cơ bản của tài khoản!");
        }

        user.removeRole(role);
        if (role == Role.MANAGER) {
            user.setStore(null);
        }
        userRepository.save(user);

        RoleAuditLog logEntry = RoleAuditLog.builder()
                .user(user)
                .action("REVOKE")
                .role(role)
                .store(null)
                .reason(reason != null && !reason.trim().isEmpty() ? reason.trim() : "Quản trị viên thu hồi vai trò")
                .performedBy(admin)
                .createdAt(LocalDateTime.now())
                .build();
        roleAuditLogRepository.save(logEntry);

        log.info("Admin {} đã thu hồi vai trò {} của user {}", admin.getUsername(), role, user.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleAuditLog> getAuditLogsForUser(Long userId) {
        return roleAuditLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    private void requireAdmin(User admin) {
        if (!admin.hasRole(Role.ADMIN) || !Boolean.TRUE.equals(admin.getIsActive())
                || Boolean.TRUE.equals(admin.getAccountDisabledByAdmin())) {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ quản trị viên đang hoạt động được thay đổi quyền.");
        }
    }

    private void validateLength(String value, int max, String field) {
        if (value != null && value.trim().length() > max)
            throw new IllegalArgumentException(field + " không được vượt quá " + max + " ký tự.");
    }

}
