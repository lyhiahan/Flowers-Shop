package com.finalproject.controller.common;
import com.finalproject.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import java.security.Principal;
import java.util.Map;
@RestController @RequestMapping("/api/chat") @RequiredArgsConstructor
public class ChatController {
 private final ChatService chat;
 public record OpenRequest(String kind,Long storeId){}
 @GetMapping("/context") public ChatService.Context context(Principal principal){return chat.context(principal.getName());}
 @GetMapping("/conversations") public ChatService.Inbox inbox(Principal principal,@RequestParam(defaultValue="0") int page){return chat.inbox(principal.getName(),page);}
 @PostMapping("/conversations") public ChatService.ConversationView open(Principal principal,@RequestBody OpenRequest input){return chat.open(principal.getName(),input.kind(),input.storeId());}
 @GetMapping("/conversations/{id}/messages") public ChatService.History history(Principal principal,@PathVariable Long id,@RequestParam(required=false) Long before){return chat.history(principal.getName(),id,before);}
 @PostMapping(value="/conversations/{id}/images",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public ChatService.MessageView image(Principal principal,@PathVariable Long id,@RequestParam String clientMessageId,
        @RequestParam(defaultValue="") String content,@RequestParam org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
  return chat.sendImage(principal.getName(),id,content,clientMessageId,file);
 }
 @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class) public ResponseEntity<Map<String,String>> oversized(Exception ex){return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of("error","Ảnh tối đa 10 MB."));}
 @ExceptionHandler(AccessDeniedException.class) public ResponseEntity<Map<String,String>> denied(AccessDeniedException ex){return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error",ex.getMessage()));}
 @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<Map<String,String>> invalid(IllegalArgumentException ex){return ResponseEntity.badRequest().body(Map.of("error",ex.getMessage()));}
 @ExceptionHandler({IllegalStateException.class,org.springframework.dao.PessimisticLockingFailureException.class}) public ResponseEntity<Map<String,String>> conflict(Exception ex){return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error",ex instanceof IllegalStateException?ex.getMessage():"Hội thoại đang được xử lý. Vui lòng thử lại."));}
}
