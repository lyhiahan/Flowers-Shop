package com.finalproject.repository;
import com.finalproject.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.*;
public interface ChatMessageRepository extends JpaRepository<ChatMessage,Long> {
 Optional<ChatMessage> findByConversationIdAndSenderIdAndClientMessageId(Long conversation,Long sender,String clientId);
 List<ChatMessage> findByConversationIdOrderByIdDesc(Long conversation,Pageable pageable);
 List<ChatMessage> findByConversationIdAndIdLessThanOrderByIdDesc(Long conversation,Long before,Pageable pageable);
}
