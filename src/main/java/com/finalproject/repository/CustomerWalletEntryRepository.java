package com.finalproject.repository;

import com.finalproject.entity.CustomerWalletEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CustomerWalletEntryRepository extends JpaRepository<CustomerWalletEntry, Long> {
    Optional<CustomerWalletEntry> findByOrderIdAndType(Long orderId, String type);
    Page<CustomerWalletEntry> findByWalletUserIdOrderByIdDesc(Long userId, Pageable pageable);
}
