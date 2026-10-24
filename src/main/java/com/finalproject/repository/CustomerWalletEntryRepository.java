package com.finalproject.repository;

import com.finalproject.entity.CustomerWalletEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;

public interface CustomerWalletEntryRepository extends JpaRepository<CustomerWalletEntry, Long> {
    Optional<CustomerWalletEntry> findByOrderIdAndType(Long orderId, String type);
    Page<CustomerWalletEntry> findByWalletUserIdOrderByIdDesc(Long userId, Pageable pageable);

    @Query("""
            select e from CustomerWalletEntry e
            where e.wallet.userId = :userId
              and (:fromTime is null or e.createdAt >= :fromTime)
              and (:untilTime is null or e.createdAt < :untilTime)
            order by e.id desc
            """)
    Page<CustomerWalletEntry> findHistory(@Param("userId") Long userId,
            @Param("fromTime") LocalDateTime fromTime, @Param("untilTime") LocalDateTime untilTime,
            Pageable pageable);
}
