package com.finalproject.repository;

import com.finalproject.entity.RoleRequest;
import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.enums.RoleRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleRequestRepository extends JpaRepository<RoleRequest, Long> {

    List<RoleRequest> findByUserOrderByCreatedAtDesc(User user);

    List<RoleRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<RoleRequest> findAllByOrderByCreatedAtDesc();

    List<RoleRequest> findByStatusOrderByCreatedAtDesc(RoleRequestStatus status);

    boolean existsByUserIdAndTargetRoleAndStatus(Long userId, Role targetRole, RoleRequestStatus status);

    long countByStatus(RoleRequestStatus status);
}
