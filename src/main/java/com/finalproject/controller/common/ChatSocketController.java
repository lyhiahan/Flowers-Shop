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
 public record SendRequest(Long conversationId,String content,String clientMessageId,Long productId){}
 @MessageMapping("/chat.send")
 @SendToUser(destinations="/queue/chat.acks",broadcast=false)
 public ChatService.MessageView send(SendRequest input,Principal principal,@Header("simpSessionAttributes") Map<String,Object> attributes){
  Long storeId=attributes.get("chatStoreId") instanceof Long id?id:null;
  var actor=chat.activeUser(principal==null?null:principal.getName());
  if(actor.isStoreWorker() && !actor.hasRole(com.finalproject.enums.Role.ADMIN) && storeId==null)
   throw new org.springframework.security.access.AccessDeniedException("Phiên chat quản lý chưa chọn chi nhánh.");
  return chat.send(principal.getName(),input.conversationId(),input.content(),input.clientMessageId(),storeId,input.productId());
 }
 @MessageExceptionHandler({IllegalArgumentException.class,IllegalStateException.class,org.springframework.security.access.AccessDeniedException.class})
 @SendToUser(destinations="/queue/chat.errors",broadcast=false)
 public Map<String,String> error(Exception ex){return Map.of("error",ex.getMessage());}
}
