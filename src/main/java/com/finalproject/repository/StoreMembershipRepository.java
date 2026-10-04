package com.finalproject.repository;

import com.finalproject.entity.StoreMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.*;
import java.util.*;

public interface StoreMembershipRepository extends JpaRepository<StoreMembership, Long> {
    Optional<StoreMembership> findByUserIdAndStoreId(Long userId, Long storeId);
    List<StoreMembership> findByUserIdAndActiveTrue(Long userId);
    @Query("select m from StoreMembership m join fetch m.user where m.store.id = :storeId and m.active = true")
    Page<StoreMembership> findActiveByStore(Long storeId, Pageable pageable);
    boolean existsByUserIdAndActiveTrue(Long userId);
}
