package com.finalproject.repository;
import com.finalproject.entity.ChatConversation;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface ChatConversationRepository extends JpaRepository<ChatConversation,Long>, JpaSpecificationExecutor<ChatConversation> {
 Optional<ChatConversation> findByConversationKey(String key);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from ChatConversation c where c.id=:id")
 Optional<ChatConversation> findLockedById(@Param("id") Long id);
}
