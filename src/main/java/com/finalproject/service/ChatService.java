package com.finalproject.service;
import com.finalproject.entity.*;
import com.finalproject.enums.Role;
import com.finalproject.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor @lombok.extern.slf4j.Slf4j
public class ChatService {
 private final UserRepository users;
 private final StoreRepository stores;
 private final StoreMembershipRepository memberships;
 private final ChatConversationRepository conversations;
 private final ChatMessageRepository messages;
 private final SimpMessagingTemplate broker;
 private final Clock clock;
 private final CloudinaryService cloud;
 public record ConversationView(Long id,String kind,Long storeId,String storeName,Long customerId,String customerName,String lastMessage,
  @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) LocalDateTime updatedAt){}
 public record MessageView(Long id,Long conversationId,Long senderId,String senderName,String content,String imageUrl,String clientMessageId,
  @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) LocalDateTime createdAt){}
 public record History(List<MessageView> messages,boolean hasMore){}
 public record Inbox(List<ConversationView> conversations,boolean hasMore){}
 public record StoreChoice(Long id,String name){}
 public record Context(Long userId,String role,List<StoreChoice> stores){}
 public record Event(String type,ConversationView conversation,MessageView message){}
 public User activeUser(String username) {
  if(username==null) throw new AccessDeniedException("Vui lòng đăng nhập để nhắn tin.");
  return users.findByUsername(username).filter(u->Boolean.TRUE.equals(u.getIsActive()) && !Boolean.TRUE.equals(u.getAccountDisabledByAdmin()) && !u.isRetiredDeliveryAccount())
    .orElseThrow(()->new AccessDeniedException("Tài khoản không còn quyền nhắn tin."));
 }
 private List<User> managers(Long storeId) {
  return users.findChatManagers(storeId,Role.MANAGER).stream().filter(u->Boolean.TRUE.equals(u.getIsActive()) && !u.isRetiredDeliveryAccount()).toList();
 }
 private List<User> admins() {
  return users.findByRole(Role.ADMIN).stream().filter(u->Boolean.TRUE.equals(u.getIsActive()) && !Boolean.TRUE.equals(u.getAccountDisabledByAdmin()) && !u.isRetiredDeliveryAccount()).toList();
 }
 private boolean managesStore(User user,Store store) {
  return store!=null && user.hasRole(Role.MANAGER)
    && managers(store.getId()).stream().anyMatch(manager->manager.getId().equals(user.getId()));
 }
 private void rejectSelfConversation(User user,ChatConversation conversation) {
  if("STORE".equals(conversation.getKind()) && user.getId().equals(conversation.getCustomer().getId())
     && managesStore(user,conversation.getStore())) {
   throw new IllegalArgumentException("Bạn không thể nhắn tin cho chi nhánh do chính mình quản lý.");
  }
 }
 private void authorize(User user,ChatConversation conversation) {
  if(user.getId().equals(conversation.getCustomer().getId()) && !user.hasRole(Role.ADMIN)) return;
  if("ADMIN".equals(conversation.getKind()) && user.hasRole(Role.ADMIN)) return;
  if("STORE".equals(conversation.getKind()) && user.hasRole(Role.MANAGER) && !user.hasRole(Role.ADMIN)
     && managers(conversation.getStore().getId()).stream().anyMatch(u->u.getId().equals(user.getId()))) return;
  throw new AccessDeniedException("Bạn không có quyền truy cập cuộc trò chuyện này.");
 }
 @Transactional(readOnly=true)
 public Context context(String username) {
  User user=activeUser(username);
  var choices=stores.findByStatusTrue().stream().filter(s->!managesStore(user,s)).map(s->new StoreChoice(s.getId(),s.getName())).toList();
  return new Context(user.getId(),user.hasRole(Role.ADMIN)?"ADMIN":user.hasRole(Role.MANAGER)?"MANAGER":"USER",choices);
 }
 @Transactional
 public ConversationView open(String username,String kind,Long storeId) {
  User user=activeUser(username);
  if(user.hasRole(Role.ADMIN) || (!user.hasRole(Role.USER) && !user.hasRole(Role.MANAGER))) throw new AccessDeniedException("Chỉ tài khoản người dùng được mở hội thoại hỗ trợ.");
  user=users.findLockedById(user.getId()).orElseThrow();
  if(!"STORE".equals(kind) && !"ADMIN".equals(kind)) throw new IllegalArgumentException("Vui lòng chọn chi nhánh hoặc Admin.");
  Store store=null;
  if("STORE".equals(kind)) {
   if(storeId==null) throw new IllegalArgumentException("Vui lòng chọn chi nhánh cần nhắn tin.");
   store=stores.findById(storeId).filter(s->Boolean.TRUE.equals(s.getStatus())).orElseThrow(()->new IllegalArgumentException("Chi nhánh không còn hoạt động."));
   if(managesStore(user,store)) throw new IllegalArgumentException("Bạn không thể nhắn tin cho chi nhánh do chính mình quản lý.");
   if(managers(storeId).isEmpty()) throw new IllegalStateException("Chi nhánh chưa có quản lý hoạt động. Bạn có thể nhắn Admin.");
  } else if(admins().isEmpty()) throw new IllegalStateException("Chưa có tài khoản Admin hoạt động.");
  String key=kind+":"+user.getId()+("STORE".equals(kind)?":"+storeId:"");
  var existing=conversations.findByConversationKey(key);
  if(existing.isPresent()) return view(existing.get());
  LocalDateTime now=LocalDateTime.now(clock);
  var created=conversations.save(ChatConversation.builder().conversationKey(key).kind(kind).customer(user).store(store).createdAt(now).updatedAt(now).build());
  return view(created);
 }
 @Transactional(readOnly=true)
 public Inbox inbox(String username,int page) {
  User user=activeUser(username);
  if(page<0 || page>10000) throw new IllegalArgumentException("Trang hội thoại không hợp lệ.");
  Set<Long> managed=new HashSet<>();
  if(user.hasRole(Role.MANAGER) && !user.hasRole(Role.ADMIN)) {
   stores.findByOwnerId(user.getId()).forEach(s->managed.add(s.getId()));
   memberships.findByUserIdAndActiveTrue(user.getId()).forEach(m->managed.add(m.getStore().getId()));
   if(user.getStore()!=null && managers(user.getStore().getId()).stream().anyMatch(m->m.getId().equals(user.getId()))) managed.add(user.getStore().getId());
  }
  var result=conversations.findAll((root,query,cb)-> {
   if(user.hasRole(Role.ADMIN)) return cb.equal(root.get("kind"),"ADMIN");
   var own=cb.equal(root.get("customer").get("id"),user.getId());
   if(managed.isEmpty()) return own;
   var branch=root.join("store",jakarta.persistence.criteria.JoinType.LEFT);
   return cb.or(own,cb.and(cb.equal(root.get("kind"),"STORE"),branch.get("id").in(managed)));
  },PageRequest.of(page,30,Sort.by(Sort.Direction.DESC,"updatedAt","id")));
  // Verify current membership as well as the scoped query.
  var visible=result.getContent().stream().filter(c->{try{authorize(user,c);return true;}catch(AccessDeniedException e){return false;}}).map(this::view).toList();
  return new Inbox(visible,result.hasNext());
 }
 @Transactional(readOnly=true)
 public History history(String username,Long conversationId,Long before) {
  User user=activeUser(username);
  var conversation=conversations.findById(conversationId).orElseThrow(()->new IllegalArgumentException("Không tìm thấy hội thoại."));
  authorize(user,conversation);
  if(before!=null && before<=0) throw new IllegalArgumentException("Mốc lịch sử không hợp lệ.");
  var batch=before==null?messages.findByConversationIdOrderByIdDesc(conversationId,PageRequest.of(0,51)):
       messages.findByConversationIdAndIdLessThanOrderByIdDesc(conversationId,before,PageRequest.of(0,51));
  boolean more=batch.size()>50;
  var list=new ArrayList<>(batch.subList(0,Math.min(50,batch.size())).stream().map(this::messageView).toList());
  Collections.reverse(list);
  return new History(list,more);
 }
 @Transactional
 public MessageView send(String username,Long conversationId,String content,String clientMessageId) {
  return saveMessage(username,conversationId,content,clientMessageId,null);
 }
 @Transactional(rollbackFor=java.io.IOException.class)
 public MessageView sendImage(String username,Long conversationId,String content,String clientMessageId,org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
  if(file==null || file.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn ảnh để gửi.");
  return saveMessage(username,conversationId,content,clientMessageId,file);
 }
 private MessageView saveMessage(String username,Long conversationId,String content,String clientMessageId,org.springframework.web.multipart.MultipartFile image) {
  User user=activeUser(username);
  if(conversationId==null) throw new IllegalArgumentException("Vui lòng chọn cuộc trò chuyện.");
  String text=content==null?"":content.strip();
  if((text.isBlank() && image==null) || text.length()>2000 || text.indexOf('\0')>=0) throw new IllegalArgumentException("Tin nhắn phải có nội dung hoặc ảnh, tối đa 2.000 ký tự.");
  try {if(clientMessageId==null || !UUID.fromString(clientMessageId).toString().equals(clientMessageId)) throw new IllegalArgumentException();}
  catch(IllegalArgumentException ex){throw new IllegalArgumentException("Mã tin nhắn không hợp lệ.");}
  var conversation=conversations.findLockedById(conversationId).orElseThrow(()->new IllegalArgumentException("Không tìm thấy hội thoại."));
  authorize(user,conversation);
  rejectSelfConversation(user,conversation);
  var duplicate=messages.findByConversationIdAndSenderIdAndClientMessageId(conversationId,user.getId(),clientMessageId);
  if(duplicate.isPresent()) {
   if(!duplicate.get().getContent().equals(text) || (duplicate.get().getImageUrl()!=null)!=(image!=null)) throw new IllegalArgumentException("Mã tin nhắn đã được sử dụng cho nội dung khác.");
   var result=messageView(duplicate.get()); publish(List.of(user),new Event("MESSAGE",view(conversation),result)); return result;
  }
  LocalDateTime now=LocalDateTime.now(clock);
  CloudinaryService.UploadResult uploaded=null;
  if(image!=null) {
   try {uploaded=cloud.uploadToCloud(image,"flower_shop/chat/"+conversationId);}
   catch(java.io.IOException ex){throw new IllegalStateException(ex.getMessage(),ex);}
   if(uploaded==null || uploaded.secureUrl()==null || !uploaded.secureUrl().startsWith("https://")) throw new IllegalStateException("Cloudinary không trả về ảnh hợp lệ.");
  }
  var message=messages.save(ChatMessage.builder().conversation(conversation).sender(user).content(text).clientMessageId(clientMessageId).createdAt(now)
      .imageUrl(uploaded==null?null:uploaded.secureUrl()).imagePublicId(uploaded==null?null:uploaded.publicId()).build());
  conversation.setLastMessage(text.isEmpty()?"[Ảnh]":text);conversation.setUpdatedAt(now);conversations.save(conversation);
  var recipients=new ArrayList<User>();recipients.add(conversation.getCustomer());
  recipients.addAll("ADMIN".equals(conversation.getKind())?admins():managers(conversation.getStore().getId()));
  var result=messageView(message);
  publish(recipients,new Event("MESSAGE",view(conversation),result));
  return result;
 }
 private void publish(List<User> recipients,Event event) {
  var usernames=recipients.stream().filter(u->Boolean.TRUE.equals(u.getIsActive()) && !Boolean.TRUE.equals(u.getAccountDisabledByAdmin()) && !u.isRetiredDeliveryAccount()).map(User::getUsername).distinct().toList();
  TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
   @Override public void afterCommit(){usernames.forEach(name->{
    try {broker.convertAndSendToUser(name,"/queue/chat",event);}
    catch(RuntimeException ex){log.warn("Tin nhắn {} đã lưu nhưng chưa phát được tới {}",event.message().id(),name,ex);}
   });}
  });
 }
 private ConversationView view(ChatConversation c) {
  return new ConversationView(c.getId(),c.getKind(),c.getStore()==null?null:c.getStore().getId(),c.getStore()==null?null:c.getStore().getName(),c.getCustomer().getId(),c.getCustomer().getFullName(),c.getLastMessage(),c.getUpdatedAt());
 }
 private MessageView messageView(ChatMessage m) {
  return new MessageView(m.getId(),m.getConversation().getId(),m.getSender().getId(),m.getSender().getFullName(),m.getContent(),m.getImageUrl(),m.getClientMessageId(),m.getCreatedAt());
 }
}
