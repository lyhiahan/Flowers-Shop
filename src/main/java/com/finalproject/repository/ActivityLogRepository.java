package com.finalproject.repository;

import com.finalproject.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findByUserIdOrderByTimestampDesc(Long userId);
    List<ActivityLog> findByTargetTypeAndTargetId(String targetType, Long targetId);
}