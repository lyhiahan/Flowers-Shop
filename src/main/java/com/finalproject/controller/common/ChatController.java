package com.finalproject.controller.common;
import com.finalproject.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import java.security.Principal;
import java.util.Map;
@RestController @RequestMapping("/api/chat") @RequiredArgsConstructor @lombok.extern.slf4j.Slf4j
public class ChatController {
 private final ChatService chat;
 public record OpenRequest(String kind,Long storeId){}
 public record ContactRequest(Long productId,Long orderId,Long storeId){}
 @GetMapping("/context") public ChatService.Context context(Principal principal){return chat.context(principal.getName());}
 @GetMapping("/conversations") public ChatService.Inbox inbox(Principal principal,@RequestParam(defaultValue="0") int page,@RequestParam(required=false) Long storeId,
  @RequestParam(required=false) String kind,@RequestParam(required=false) String keyword,
  @RequestParam(required=false) @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate from,
  @RequestParam(required=false) @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate to){return chat.inbox(principal.getName(),page,storeId,kind,keyword,from,to);}
 @PostMapping("/conversations") public ChatService.ConversationView open(Principal principal,@RequestBody OpenRequest input){return chat.open(principal.getName(),input.kind(),input.storeId());}
 @PostMapping("/conversations/contact") public ChatService.Contact contact(Principal principal,@RequestBody ContactRequest input){return chat.contact(principal.getName(),input.productId(),input.orderId(),input.storeId());}
 @GetMapping("/conversations/{id}/messages") public ResponseEntity<ChatService.History> history(Principal principal,@PathVariable Long id,@RequestParam(required=false) Long before,@RequestParam(required=false) Long storeId){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(chat.history(principal.getName(),id,before,storeId));}
 @GetMapping("/access-logs") public org.springframework.data.domain.Page<ChatService.AuditView> logs(Principal principal,@RequestParam(defaultValue="0") int page){return chat.accessLogs(principal.getName(),page);}
 @PostMapping(value="/conversations/{id}/images",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public ChatService.MessageView image(Principal principal,@PathVariable Long id,@RequestParam String clientMessageId,
        @RequestParam(defaultValue="") String content,@RequestParam org.springframework.web.multipart.MultipartFile file,@RequestParam(required=false) Long storeId,@RequestParam(required=false) Long productId) throws java.io.IOException {
  return chat.sendImage(principal.getName(),id,content,clientMessageId,file,storeId,productId);
 }
 @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class) public ResponseEntity<Map<String,String>> oversized(Exception ex){return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(Map.of("error","Ảnh tối đa 10 MB."));}
 @ExceptionHandler(AccessDeniedException.class) public ResponseEntity<Map<String,String>> denied(AccessDeniedException ex){return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error",ex.getMessage()));}
 @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<Map<String,String>> invalid(IllegalArgumentException ex){return ResponseEntity.badRequest().body(Map.of("error",ex.getMessage()));}
 @ExceptionHandler({org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,org.springframework.http.converter.HttpMessageNotReadableException.class})
 public ResponseEntity<Map<String,String>> malformed(Exception ex){return ResponseEntity.badRequest().body(Map.of("error","Tham số hội thoại/ngày không hợp lệ."));}
 @ExceptionHandler({IllegalStateException.class,org.springframework.dao.PessimisticLockingFailureException.class}) public ResponseEntity<Map<String,String>> conflict(Exception ex){return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error",ex instanceof IllegalStateException?ex.getMessage():"Hội thoại đang được xử lý. Vui lòng thử lại."));}
 @ExceptionHandler(org.springframework.dao.DataAccessException.class)
 public ResponseEntity<Map<String,String>> storage(Exception ex){
  log.error("Không thể truy cập dữ liệu chat. Kiểm tra Flyway và migration V4/V5.",ex);
  return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error","Chat tạm thời chưa khả dụng. Vui lòng thử lại sau.","code","CHAT_STORAGE_UNAVAILABLE"));
 }
 @ExceptionHandler(Exception.class)
 public ResponseEntity<Map<String,String>> unexpected(Exception ex){
  log.error("Không thể xử lý yêu cầu chat.",ex);
  return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error","Không thể tải chat lúc này. Vui lòng thử lại sau."));
 }
}
