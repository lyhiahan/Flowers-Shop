package com.finalproject.repository;

import com.finalproject.entity.OtpVerification;
import com.finalproject.enums.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    Optional<OtpVerification> findTopByUserIdAndTypeOrderByExpiredAtDesc(Long userId, OtpType type);
    // Tìm mã OTP mới nhất chưa sử dụng của user theo loại
    Optional<OtpVerification> findTopByUserIdAndTypeAndIsUsedFalseOrderByExpiredAtDesc(
            Long userId, OtpType type);

    // Tìm mã OTP mới nhất chưa sử dụng của user
    Optional<OtpVerification> findTopByUserIdAndCodeAndTypeAndIsUsedFalseOrderByExpiredAtDesc(
            Long userId, String code, OtpType type);

    // Đếm số lần gửi OTP trong khoảng thời gian để chống spam
    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime timeLimit);
}
