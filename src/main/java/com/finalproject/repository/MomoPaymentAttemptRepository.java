package com.finalproject.repository;

import com.finalproject.entity.MomoPaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MomoPaymentAttemptRepository extends JpaRepository<MomoPaymentAttempt, Long> {
    Optional<MomoPaymentAttempt> findByGatewayOrderId(String gatewayOrderId);
}
