package com.finalproject.service;

import com.finalproject.entity.RoleAuditLog;
import com.finalproject.entity.RoleRequest;
import com.finalproject.enums.Role;
import com.finalproject.enums.RoleRequestStatus;

import java.util.List;

public interface RoleManagementService {

    RoleRequest submitRoleRequest(Long userId,
                                  Role targetRole,
                                  String phone,
                                  Long targetStoreId,
                                  String shopName,
                                  String shopAddress,
                                  String contactInfo,
                                  String experienceNote);

    void cancelRoleRequest(Long requestId, Long userId);

    List<RoleRequest> getUserRoleRequests(Long userId);

    List<RoleRequest> getAllRoleRequests(RoleRequestStatus status);

    RoleRequest approveRoleRequest(Long requestId, Long adminId, Long assignedStoreId);

    RoleRequest rejectRoleRequest(Long requestId, Long adminId, String reason);

    void directlyAssignRole(Long userId, Role role, Long storeId, String reason, Long adminId);

    void directlyRevokeRole(Long userId, Role role, String reason, Long adminId);

    List<RoleAuditLog> getAuditLogsForUser(Long userId);
}
