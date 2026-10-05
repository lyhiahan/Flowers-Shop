package com.finalproject.controller.common;
import com.finalproject.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import java.util.Map;
@Controller @RequiredArgsConstructor
public class ChatSocketController {
 private final ChatService chat;
 public record SendRequest(Long conversationId,String content,String clientMessageId){}
 @MessageMapping("/chat.send")
 public void send(SendRequest input,Principal principal){chat.send(principal==null?null:principal.getName(),input.conversationId(),input.content(),input.clientMessageId());}
 @MessageExceptionHandler({IllegalArgumentException.class,IllegalStateException.class,org.springframework.security.access.AccessDeniedException.class})
 @SendToUser(destinations="/queue/chat.errors",broadcast=false)
 public Map<String,String> error(Exception ex){return Map.of("error",ex.getMessage());}
}
