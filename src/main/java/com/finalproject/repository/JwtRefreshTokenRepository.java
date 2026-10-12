package com.finalproject.repository;

import com.finalproject.entity.JwtRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JwtRefreshTokenRepository extends JpaRepository<JwtRefreshToken, String> {}
