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
 private final StoreAccessService storeAccess;
 private final ChatConversationRepository conversations;
 private final ChatMessageRepository messages;
 private final SimpMessagingTemplate broker;
 private final org.springframework.messaging.simp.user.SimpUserRegistry socketUsers;
 private final Clock clock;
 private final CloudinaryService cloud;
 private final ProductRepository products;
 private final OrderRepository orders;
 private final StoreInventoryRepository inventories;
 private final ChatAccessAuditRepository audits;
 private final ChatProductContextRepository productContexts;
 public record ProductView(Long id,String name,String imageUrl,java.math.BigDecimal price){}
 public record ConversationView(Long id,String kind,Long storeId,String storeName,Long customerId,String customerName,String lastMessage,
  @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) LocalDateTime updatedAt){}
 public record MessageView(Long id,Long conversationId,Long senderId,String senderName,String content,String imageUrl,String clientMessageId,
  @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) LocalDateTime createdAt,ProductView product){}
 public record History(List<MessageView> messages,boolean hasMore){}
 public record Inbox(List<ConversationView> conversations,boolean hasMore){}
 public record StoreChoice(Long id,String name){}
 public record Context(Long userId,String role,List<StoreChoice> stores,Long storeId,String storeName){}
 public record Contact(ConversationView conversation,String suggestedMessage,ProductView product){}
 public record Event(String type,ConversationView conversation,MessageView message){}
 public record AuditView(Long id,String administrator,Long conversationId,String storeName,String customerName,LocalDateTime accessedAt,Long beforeMessageId){}
 public User activeUser(String username) {
  if(username==null) throw new AccessDeniedException("Vui lòng đăng nhập để nhắn tin.");
  return users.findByUsername(username).filter(u->Boolean.TRUE.equals(u.getIsActive()) && !Boolean.TRUE.equals(u.getAccountDisabledByAdmin()) && !u.isRetiredDeliveryAccount())
    .orElseThrow(()->new AccessDeniedException("Tài khoản không còn quyền nhắn tin."));
 }
 private List<User> managers(Long storeId) {
  return java.util.stream.Stream.concat(users.findChatManagers(storeId,Role.MANAGER).stream(), users.findChatManagers(storeId,Role.STAFF).stream()).distinct().filter(u->storeAccess.managesStore(u,storeId)).toList();
 }
 private List<User> admins() {
  return users.findByRole(Role.ADMIN).stream().filter(u->Boolean.TRUE.equals(u.getIsActive()) && !Boolean.TRUE.equals(u.getAccountDisabledByAdmin()) && !u.isRetiredDeliveryAccount()).toList();
 }
 private boolean managesStore(User user,Store store) {
  return store!=null && storeAccess.managesStore(user,store.getId());
 }
 private void rejectSelfConversation(User user,ChatConversation conversation) {
  if("STORE".equals(conversation.getKind()) && user.getId().equals(conversation.getCustomer().getId())
     && managesStore(user,conversation.getStore())) {
   throw new IllegalArgumentException("Bạn không thể nhắn tin cho chi nhánh do chính mình quản lý.");
  }
 }
 private Store managerStore(User user,Long requestedId) {
  if(requestedId==null) {
   var attributes=org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
   if(attributes instanceof org.springframework.web.context.request.ServletRequestAttributes servlet) {
    var session=servlet.getRequest().getSession(false);
    if(session!=null && session.getAttribute("activeStoreId") instanceof Long id) requestedId=id;
   }
  }
  if(requestedId!=null) {
   Store selected=stores.findById(requestedId).orElseThrow(()->new AccessDeniedException("Chi nhánh chat không hợp lệ."));
   if(!managesStore(user,selected)) throw new AccessDeniedException("Bạn không có quyền xem hộp thư chi nhánh này.");
   return selected;
  }
  if(user.getStore()!=null && managesStore(user,user.getStore())) return user.getStore();
  var owned=stores.findByOwnerId(user.getId()).stream().filter(s->managesStore(user,s)).sorted(Comparator.comparing(Store::getId)).findFirst();
  if(owned.isPresent()) return owned.get();
  return memberships.findByUserIdAndActiveTrue(user.getId()).stream().map(StoreMembership::getStore)
   .filter(s->managesStore(user,s)).sorted(Comparator.comparing(Store::getId)).findFirst()
   .orElseThrow(()->new AccessDeniedException("Tài khoản chưa được phân công chi nhánh để nhận tin khách hàng."));
 }
 private void authorize(User user,ChatConversation conversation,Long scopeStoreId) {
  if(user.hasRole(Role.ADMIN) && ("ADMIN".equals(conversation.getKind()) || "BRANCH_SUPPORT".equals(conversation.getKind()))) return;
  if(user.hasRole(Role.ADMIN)) throw new AccessDeniedException("Hội thoại khách–chi nhánh chỉ được giám sát, không được gửi tin thay cửa hàng.");
  if(user.isStoreWorker()) {
   Store selected=managerStore(user,scopeStoreId);
   if("BRANCH_SUPPORT".equals(conversation.getKind()) && user.hasRole(Role.MANAGER) && selected.getId().equals(conversation.getStore().getId())) return;
   if("STORE".equals(conversation.getKind()) && selected.getId().equals(conversation.getStore().getId())
      && !user.getId().equals(conversation.getCustomer().getId())) return;
   throw new AccessDeniedException("Hội thoại không thuộc hộp thư khách hàng của chi nhánh đang chọn.");
  }
  if(user.getId().equals(conversation.getCustomer().getId()) && List.of("STORE","ADMIN").contains(conversation.getKind())) return;
  throw new AccessDeniedException("Bạn không có quyền truy cập cuộc trò chuyện này.");
 }
 @Transactional(readOnly=true)
 public Context context(String username) {
  User user=activeUser(username);
  String role=user.hasRole(Role.ADMIN)?"ADMIN":user.hasRole(Role.MANAGER)?"MANAGER":user.hasRole(Role.STAFF)?"STAFF":"USER";
  Store selected=user.isStoreWorker() && !user.hasRole(Role.ADMIN)?managerStore(user,null):null;
  return new Context(user.getId(),role,List.of(),selected==null?null:selected.getId(),selected==null?null:selected.getName());
 }
 @Transactional
 public ConversationView open(String username,String kind,Long storeId) {
  User user=activeUser(username);
  boolean internal="BRANCH_SUPPORT".equals(kind);
  if(internal) {
   if(!user.hasRole(Role.MANAGER) || user.hasRole(Role.ADMIN)) throw new AccessDeniedException("Chỉ quản lý được mở hỗ trợ nội bộ chi nhánh.");
   storeId=managerStore(user,storeId).getId();
   if(admins().isEmpty()) throw new IllegalStateException("Chưa có nhân viên hỗ trợ toàn chuỗi hoạt động.");
  } else if(user.hasRole(Role.ADMIN) || user.isStoreWorker() || !user.hasRole(Role.USER)) throw new AccessDeniedException("Chỉ khách hàng được mở hội thoại. Quản lý chỉ tiếp nhận tin của chi nhánh mình.");
  user=users.findLockedById(user.getId()).orElseThrow();
  if(!"STORE".equals(kind) && !"ADMIN".equals(kind) && !internal) throw new IllegalArgumentException("Kênh hội thoại không hợp lệ.");
  Store store=internal?managerStore(user,storeId):null;
  // Different managers share one internal thread: serialize creation per branch,
  // not only per initiating user, so simultaneous opens cannot race the unique key.
  if(internal) store=stores.findLockedById(storeId).orElseThrow(()->new AccessDeniedException("Chi nhánh chat không hợp lệ."));
  if("STORE".equals(kind)) {
   if(storeId==null) throw new IllegalArgumentException("Vui lòng chọn chi nhánh cần nhắn tin.");
   store=stores.findById(storeId).filter(s->Boolean.TRUE.equals(s.getStatus())).orElseThrow(()->new IllegalArgumentException("Chi nhánh không còn hoạt động."));
   if(managesStore(user,store)) throw new IllegalArgumentException("Bạn không thể nhắn tin cho chi nhánh do chính mình quản lý.");
   if(managers(storeId).isEmpty()) throw new IllegalStateException("Chi nhánh chưa có quản lý hoạt động. Bạn có thể nhắn Admin.");
  } else if(admins().isEmpty()) throw new IllegalStateException("Chưa có tài khoản Admin hoạt động.");
  String key=internal?"BRANCH_SUPPORT:"+storeId:kind+":"+user.getId()+("STORE".equals(kind)?":"+storeId:"");
  var existing=conversations.findByConversationKey(key);
  if(existing.isPresent()) return view(existing.get());
  LocalDateTime now=LocalDateTime.now(clock);
  var created=conversations.save(ChatConversation.builder().conversationKey(key).kind(kind).customer(user).store(store).createdAt(now).updatedAt(now).build());
  return view(created);
 }
 @Transactional
 public Contact contact(String username,Long productId,Long orderId,Long storeId) {
  User user=activeUser(username);
  if(user.hasRole(Role.ADMIN) || user.isStoreWorker()) throw new AccessDeniedException("Chỉ khách hàng được liên hệ cửa hàng.");
  if((productId==null)==(orderId==null)) throw new IllegalArgumentException("Vui lòng chọn một sản phẩm hoặc đơn hàng cần liên hệ.");
  if(orderId!=null) {
   Order order=orders.findById(orderId).orElseThrow(()->new IllegalArgumentException("Không tìm thấy đơn hàng."));
   if(!user.getId().equals(order.getUser().getId())) throw new AccessDeniedException("Đơn hàng không thuộc tài khoản của bạn.");
   if(order.getPickupStore()==null) throw new IllegalStateException("Đơn hàng chưa có chi nhánh xử lý.");
   return new Contact(open(username,"STORE",order.getPickupStore().getId()),"Tôi muốn hỏi về đơn hàng #"+order.getId()+".",null);
  }
  Product product=products.findById(productId).filter(ProductAvailability::isPublic)
   .orElseThrow(()->new IllegalArgumentException("Sản phẩm không còn khả dụng."));
  Long destination=product.getStore()==null?storeId:product.getStore().getId();
  if(destination==null) throw new IllegalArgumentException("Vui lòng chọn chi nhánh bán sản phẩm trước khi liên hệ.");
  if(product.getStore()!=null && storeId!=null && !destination.equals(storeId)) throw new IllegalArgumentException("Chi nhánh không bán sản phẩm này.");
  if(product.getStore()==null && inventories.findByStoreIdAndProductId(destination,productId).isEmpty())
   throw new IllegalArgumentException("Chi nhánh không bán sản phẩm này.");
  return new Contact(open(username,"STORE",destination),"Tôi muốn hỏi về mẫu hoa này.",productView(product));
 }
 private ProductView productView(Product product) {
  return new ProductView(product.getId(),product.getName(),product.getImageUrl(),product.getSellingPrice());
 }
 private ProductView questionProduct(User user,ChatConversation conversation,Long productId) {
  if(productId==null) return null;
  if(user.hasRole(Role.ADMIN) || user.isStoreWorker() || !"STORE".equals(conversation.getKind())
      || !user.getId().equals(conversation.getCustomer().getId())) throw new AccessDeniedException("Chỉ khách được gửi sản phẩm cần hỏi tới cửa hàng.");
  var product=products.findById(productId).filter(ProductAvailability::isPublic)
    .orElseThrow(()->new IllegalArgumentException("Sản phẩm không còn khả dụng."));
  Long storeId=conversation.getStore().getId();
  if(product.getStore()!=null ? !storeId.equals(product.getStore().getId()) : inventories.findByStoreIdAndProductId(storeId,productId).isEmpty())
    throw new IllegalArgumentException("Chi nhánh hội thoại không bán sản phẩm này.");
  return productView(product);
 }
 @Transactional(readOnly=true)
 public Inbox inbox(String username,int page) {
  return inbox(username,page,null);
 }
 @Transactional(readOnly=true)
 public Inbox inbox(String username,int page,Long scopeStoreId) {
  return inbox(username,page,scopeStoreId,null,null,null,null);
 }
 @Transactional(readOnly=true)
 public Inbox inbox(String username,int page,Long scopeStoreId,String requestedKind,String keyword,LocalDate from,LocalDate to) {
  User user=activeUser(username);
  if(page<0 || page>10000) throw new IllegalArgumentException("Trang hội thoại không hợp lệ.");
  Store selected=user.isStoreWorker() && !user.hasRole(Role.ADMIN)?managerStore(user,scopeStoreId):null;
  String kind=requestedKind==null?(user.hasRole(Role.ADMIN)?"ADMIN":selected!=null?"STORE":null):requestedKind;
  if(kind!=null && !List.of("STORE","ADMIN","BRANCH_SUPPORT").contains(kind)) throw new IllegalArgumentException("Kênh hội thoại không hợp lệ.");
  if(user.hasRole(Role.STAFF) && !user.hasRole(Role.MANAGER) && !"STORE".equals(kind)) throw new AccessDeniedException("Nhân viên chỉ được hỗ trợ khách hàng của chi nhánh.");
  if(selected!=null && !List.of("STORE","BRANCH_SUPPORT").contains(kind)) throw new AccessDeniedException("Quản lý không được mở hộp thư CSKH khách hàng.");
  if(from!=null && to!=null && from.isAfter(to)) throw new IllegalArgumentException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
  if(keyword!=null && keyword.length()>100) throw new IllegalArgumentException("Từ khóa tối đa 100 ký tự.");
  String search=keyword==null?"":keyword.strip().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_");
  var result=conversations.findAll((root,query,cb)-> {
   var predicates=new ArrayList<jakarta.persistence.criteria.Predicate>();
   if(kind!=null) predicates.add(cb.equal(root.get("kind"),kind));
   var own=cb.equal(root.get("customer").get("id"),user.getId());
   var branch=root.join("store",jakarta.persistence.criteria.JoinType.LEFT);
   if(selected!=null) {
    predicates.add(cb.equal(branch.get("id"),selected.getId()));
    if("STORE".equals(kind)) predicates.add(cb.not(own));
   } else if(!user.hasRole(Role.ADMIN)) {
    predicates.add(own);predicates.add(root.get("kind").in("STORE","ADMIN"));
   } else if(scopeStoreId!=null) predicates.add(cb.equal(branch.get("id"),scopeStoreId));
   if(!search.isEmpty()) {
    var customer=root.join("customer");
    predicates.add(cb.or(cb.like(cb.lower(customer.get("fullName")),"%"+search+"%",'!'),cb.like(cb.lower(customer.get("username")),"%"+search+"%",'!')));
   }
   if(from!=null) predicates.add(cb.greaterThanOrEqualTo(root.get("updatedAt"),from.atStartOfDay()));
   if(to!=null) predicates.add(cb.lessThan(root.get("updatedAt"),to.plusDays(1).atStartOfDay()));
   return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
  },PageRequest.of(page,30,Sort.by(Sort.Direction.DESC,"updatedAt","id")));
  // Verify current membership as well as the scoped query.
  var visible=result.getContent().stream().filter(c->{try{authorizeRead(user,c,selected==null?null:selected.getId());return true;}catch(AccessDeniedException e){return false;}})
   .map(c->{var value=view(c);return user.hasRole(Role.ADMIN) && "STORE".equals(c.getKind())
     ? new ConversationView(value.id(),value.kind(),value.storeId(),value.storeName(),value.customerId(),value.customerName(),
       "Giám sát chỉ đọc — mở hội thoại để ghi nhật ký truy cập",value.updatedAt()) : value;}).toList();
  return new Inbox(visible,result.hasNext());
 }
 @Transactional
 public History history(String username,Long conversationId,Long before) {
  return history(username,conversationId,before,null);
 }
 @Transactional
 public History history(String username,Long conversationId,Long before,Long scopeStoreId) {
  User user=activeUser(username);
  var conversation=conversations.findById(conversationId).orElseThrow(()->new IllegalArgumentException("Không tìm thấy hội thoại."));
  authorizeRead(user,conversation,scopeStoreId);
  if(before!=null && before<=0) throw new IllegalArgumentException("Mốc lịch sử không hợp lệ.");
  var batch=before==null?messages.findByConversationIdOrderByIdDesc(conversationId,PageRequest.of(0,51)):
       messages.findByConversationIdAndIdLessThanOrderByIdDesc(conversationId,before,PageRequest.of(0,51));
  boolean more=batch.size()>50;
  var list=new ArrayList<>(batch.subList(0,Math.min(50,batch.size())).stream().map(this::messageView).toList());
  Collections.reverse(list);
  if(user.hasRole(Role.ADMIN) && "STORE".equals(conversation.getKind())) {
   audits.saveAndFlush(ChatAccessAudit.builder().administrator(user).conversation(conversation)
     .accessedAt(LocalDateTime.now(clock)).beforeMessageId(before).build());
  }
  return new History(list,more);
 }
 private void authorizeRead(User user,ChatConversation conversation,Long scopeStoreId) {
  if(user.hasRole(Role.ADMIN) && "STORE".equals(conversation.getKind())) return;
  authorize(user,conversation,scopeStoreId);
 }
 @Transactional(readOnly=true)
 public org.springframework.data.domain.Page<AuditView> accessLogs(String username,int page) {
  if(!activeUser(username).hasRole(Role.ADMIN)) throw new AccessDeniedException("Chỉ admin được xem nhật ký giám sát.");
  if(page<0 || page>10000) throw new IllegalArgumentException("Trang nhật ký không hợp lệ.");
  return audits.findAll(PageRequest.of(page,30,Sort.by(Sort.Direction.DESC,"accessedAt","id")))
   .map(log->new AuditView(log.getId(),log.getAdministrator().getUsername(),log.getConversation().getId(),
    log.getConversation().getStore()==null?null:log.getConversation().getStore().getName(),log.getConversation().getCustomer().getFullName(),log.getAccessedAt(),log.getBeforeMessageId()));
 }
 @Transactional
 public MessageView send(String username,Long conversationId,String content,String clientMessageId) {
  return send(username,conversationId,content,clientMessageId,null);
 }
 @Transactional
 public MessageView send(String username,Long conversationId,String content,String clientMessageId,Long scopeStoreId) {
  return send(username,conversationId,content,clientMessageId,scopeStoreId,null);
 }
 @Transactional
 public MessageView send(String username,Long conversationId,String content,String clientMessageId,Long scopeStoreId,Long productId) {
  return saveMessage(username,conversationId,content,clientMessageId,null,scopeStoreId,productId);
 }
 @Transactional(rollbackFor=java.io.IOException.class)
 public MessageView sendImage(String username,Long conversationId,String content,String clientMessageId,org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
  return sendImage(username,conversationId,content,clientMessageId,file,null);
 }
 @Transactional(rollbackFor=java.io.IOException.class)
 public MessageView sendImage(String username,Long conversationId,String content,String clientMessageId,org.springframework.web.multipart.MultipartFile file,Long scopeStoreId) throws java.io.IOException {
  return sendImage(username,conversationId,content,clientMessageId,file,scopeStoreId,null);
 }
 @Transactional(rollbackFor=java.io.IOException.class)
 public MessageView sendImage(String username,Long conversationId,String content,String clientMessageId,org.springframework.web.multipart.MultipartFile file,Long scopeStoreId,Long productId) throws java.io.IOException {
  if(file==null || file.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn ảnh để gửi.");
  return saveMessage(username,conversationId,content,clientMessageId,file,scopeStoreId,productId);
 }
 private MessageView saveMessage(String username,Long conversationId,String content,String clientMessageId,org.springframework.web.multipart.MultipartFile image,Long scopeStoreId,Long productId) {
  User user=activeUser(username);
  if(conversationId==null) throw new IllegalArgumentException("Vui lòng chọn cuộc trò chuyện.");
  String text=content==null?"":content.strip();
  if((text.isBlank() && image==null && productId==null) || text.length()>2000 || text.indexOf('\0')>=0) throw new IllegalArgumentException("Tin nhắn phải có nội dung, ảnh hoặc sản phẩm cần hỏi, tối đa 2.000 ký tự.");
  try {if(clientMessageId==null || !UUID.fromString(clientMessageId).toString().equals(clientMessageId)) throw new IllegalArgumentException();}
  catch(IllegalArgumentException ex){throw new IllegalArgumentException("Mã tin nhắn không hợp lệ.");}
  var conversation=conversations.findLockedById(conversationId).orElseThrow(()->new IllegalArgumentException("Không tìm thấy hội thoại."));
  authorize(user,conversation,scopeStoreId);
  rejectSelfConversation(user,conversation);
  var duplicate=messages.findByConversationIdAndSenderIdAndClientMessageId(conversationId,user.getId(),clientMessageId);
  if(duplicate.isPresent()) {
   Long previousProduct=duplicate.get().getProductContext()==null?null:duplicate.get().getProductContext().getProductId();
   if(!duplicate.get().getContent().equals(text) || (duplicate.get().getImageUrl()!=null)!=(image!=null) || !Objects.equals(previousProduct,productId)) throw new IllegalArgumentException("Mã tin nhắn đã được sử dụng cho nội dung khác.");
   var result=messageView(duplicate.get()); publish(List.of(user),new Event("MESSAGE",view(conversation),result)); return result;
  }
  ProductView product=questionProduct(user,conversation,productId);
  LocalDateTime now=LocalDateTime.now(clock);
  CloudinaryService.UploadResult uploaded=null;
  if(image!=null) {
   try {uploaded=cloud.uploadToCloud(image,"flower_shop/chat/"+conversationId);}
   catch(java.io.IOException ex){throw new IllegalStateException(ex.getMessage(),ex);}
   if(uploaded==null || uploaded.secureUrl()==null || !uploaded.secureUrl().startsWith("https://")) throw new IllegalStateException("Cloudinary không trả về ảnh hợp lệ.");
  }
  var message=messages.save(ChatMessage.builder().conversation(conversation).sender(user).content(text).clientMessageId(clientMessageId).createdAt(now)
      .imageUrl(uploaded==null?null:uploaded.secureUrl()).imagePublicId(uploaded==null?null:uploaded.publicId()).build());
  if(product!=null) message.setProductContext(productContexts.save(ChatProductContext.builder().message(message).productId(product.id())
    .productName(product.name()).productImageUrl(product.imageUrl()).productPrice(product.price()).build()));
  conversation.setLastMessage(text.isEmpty()?(product!=null?"[Hỏi sản phẩm] "+product.name():"[Ảnh]"):text);conversation.setUpdatedAt(now);conversations.save(conversation);
  var recipients=new ArrayList<User>();recipients.add(conversation.getCustomer());
  if("BRANCH_SUPPORT".equals(conversation.getKind())) { recipients.clear();recipients.addAll(managers(conversation.getStore().getId()));recipients.addAll(admins()); }
  else recipients.addAll("ADMIN".equals(conversation.getKind())?admins():managers(conversation.getStore().getId()).stream()
   .filter(u -> !"BRANCH_SUPPORT".equals(conversation.getKind()) || u.hasRole(Role.MANAGER)).toList());
  var result=messageView(message);
  publish(recipients,new Event("MESSAGE",view(conversation),result));
  return result;
 }
 private void publish(List<User> recipients,Event event) {
  var usernames=recipients.stream().filter(u->Boolean.TRUE.equals(u.getIsActive()) && !Boolean.TRUE.equals(u.getAccountDisabledByAdmin()) && !u.isRetiredDeliveryAccount()).map(User::getUsername).distinct().toList();
  TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
   @Override public void afterCommit(){usernames.forEach(name->{
    try {
     User recipient=activeUser(name);
     if(recipient.isStoreWorker() && !recipient.hasRole(Role.ADMIN) && "STORE".equals(event.conversation().kind()) && recipient.getId().equals(event.conversation().customerId())) return;
     String destination=recipient.isStoreWorker() && !recipient.hasRole(Role.ADMIN) && "STORE".equals(event.conversation().kind())
       ? "/queue/chat.store."+event.conversation().storeId()
       : recipient.isStoreWorker() && !recipient.hasRole(Role.ADMIN) && "BRANCH_SUPPORT".equals(event.conversation().kind())
         ? "/queue/chat.support."+event.conversation().storeId() : "/queue/chat";
     var online=socketUsers.getUser(name);
     if(online==null) return;
     for(var session:online.getSessions()) {
      if(session.getSubscriptions().stream().noneMatch(subscription->("/user"+destination).equals(subscription.getDestination()))) continue;
      // Target one subscribed session explicitly. This also avoids reusing mutable
      // ordered-broker headers across multiple connections of the same account.
      var headers=org.springframework.messaging.simp.SimpMessageHeaderAccessor.create(org.springframework.messaging.simp.SimpMessageType.MESSAGE);
      headers.setSessionId(session.getId());headers.setLeaveMutable(true);
      broker.convertAndSendToUser(session.getId(),destination,event,headers.getMessageHeaders());
     }
    }
    catch(RuntimeException ex){log.warn("Tin nhắn {} đã lưu nhưng chưa phát được tới {}",event.message().id(),name,ex);}
   });}
  });
 }
 private ConversationView view(ChatConversation c) {
  return new ConversationView(c.getId(),c.getKind(),c.getStore()==null?null:c.getStore().getId(),c.getStore()==null?null:c.getStore().getName(),c.getCustomer().getId(),c.getCustomer().getFullName(),c.getLastMessage(),c.getUpdatedAt());
 }
 private MessageView messageView(ChatMessage m) {
  var product=m.getProductContext();
  return new MessageView(m.getId(),m.getConversation().getId(),m.getSender().getId(),m.getSender().getFullName(),m.getContent(),m.getImageUrl(),m.getClientMessageId(),m.getCreatedAt(),
    product==null?null:new ProductView(product.getProductId(),product.getProductName(),product.getProductImageUrl(),product.getProductPrice()));
 }
}
