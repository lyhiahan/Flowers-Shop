package com.finalproject.repository;

import com.finalproject.entity.RoleAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleAuditLogRepository extends JpaRepository<RoleAuditLog, Long> {

    List<RoleAuditLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<RoleAuditLog> findAllByOrderByCreatedAtDesc();
}
