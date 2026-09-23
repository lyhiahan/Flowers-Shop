package com.finalproject.repository;

import com.finalproject.entity.UserFollowStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFollowStoreRepository extends JpaRepository<UserFollowStore, Long> {
    List<UserFollowStore> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<UserFollowStore> findByUserIdAndStoreId(Long userId, Long storeId);
    boolean existsByUserIdAndStoreId(Long userId, Long storeId);
    long countByStoreId(Long storeId);
}
