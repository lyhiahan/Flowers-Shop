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

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from RoleRequest r where r.id = :id")
    java.util.Optional<RoleRequest> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);

    List<RoleRequest> findByUserOrderByCreatedAtDesc(User user);

    List<RoleRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<RoleRequest> findAllByOrderByCreatedAtDesc();

    List<RoleRequest> findByStatusOrderByCreatedAtDesc(RoleRequestStatus status);

    boolean existsByUserIdAndTargetRoleAndStatus(Long userId, Role targetRole, RoleRequestStatus status);

    long countByStatus(RoleRequestStatus status);
}
