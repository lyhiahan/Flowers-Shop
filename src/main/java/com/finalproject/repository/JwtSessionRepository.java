package com.finalproject.repository;

import com.finalproject.entity.JwtSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface JwtSessionRepository extends JpaRepository<JwtSession, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from JwtSession s where s.id = :id")
    Optional<JwtSession> lockById(@Param("id") String id);
}
