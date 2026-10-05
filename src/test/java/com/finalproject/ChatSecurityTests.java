package com.finalproject;

import com.finalproject.entity.*;
import com.finalproject.enums.Role;
import com.finalproject.repository.*;
import com.finalproject.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest @ActiveProfiles("test") @Transactional
class ChatSecurityTests {
    @Autowired ChatService chat;
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired StoreMembershipRepository members;
    @Autowired ChatMessageRepository messages;
    @Autowired RoleManagementService roleManagement;
    @MockitoBean CloudinaryService cloud;
    User customer, outsider, manager, foreignManager, admin;
    Store store;
    @BeforeEach void setup() {
        customer = account("chat_user",Role.USER); outsider = account("chat_other",Role.USER);
        manager = account("chat_manager",Role.MANAGER); foreignManager = account("chat_foreign",Role.MANAGER);
        admin = account("chat_admin",Role.ADMIN);
        store=stores.save(Store.builder().name("Chat branch").address("123 Branch Street").owner(manager).status(true).isOpen(true).build());
    }
    User account(String username,Role role) {
        return users.save(User.builder().username(username).fullName(username).email(username+"@chat.test").password("test").role(role).isActive(true).build());
    }
    Long conversation() {return chat.open(customer.getUsername(),"STORE",store.getId()).id();}
    String key() {return UUID.randomUUID().toString();}
    @Test void mergedManagerApplicationsRejectDuplicatesAndOversizedInput() {
        assertThatThrownBy(()->roleManagement.submitRoleRequest(customer.getId(),Role.MANAGER,"0900000000",null,"x".repeat(256),"Address","Contact",null)).isInstanceOf(IllegalArgumentException.class);
        roleManagement.submitRoleRequest(customer.getId(),Role.MANAGER,"0900000000",store.getId(),null,null,null,null);
        assertThatThrownBy(()->roleManagement.submitRoleRequest(customer.getId(),Role.MANAGER,"0900000000",store.getId(),null,null,null,null)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void userCannotGrantManagerAuthorityThroughService() {
        assertThatThrownBy(()->roleManagement.directlyAssignRole(outsider.getId(),Role.MANAGER,store.getId(),"forged",customer.getId())).isInstanceOf(AccessDeniedException.class);
        assertThat(users.findById(outsider.getId()).orElseThrow().hasRole(Role.MANAGER)).isFalse();
    }
    @Test void onlyThreeCanonicalRolesAndLegacyValuesRemainReadable() {
        assertThat(Role.values()).containsExactly(Role.ADMIN,Role.MANAGER,Role.USER);
        var converter=new RoleConverter();
        assertThat(converter.convertToEntityAttribute("CUSTOMER")).isEqualTo(Role.USER);
        assertThat(converter.convertToEntityAttribute("VENDOR")).isEqualTo(Role.MANAGER);
        assertThat(converter.convertToEntityAttribute("STAFF")).isEqualTo(Role.MANAGER);
    }
    @Test void reopenReusesConversationAndOwnerWithoutAssignedStoreCanReply() {
        Long id=conversation(); assertThat(conversation()).isEqualTo(id);
        chat.send(manager.getUsername(),id,"Xin chào khách hàng",key());
        assertThat(chat.history(customer.getUsername(),id,null).messages()).hasSize(1);
    }
    @Test void foreignUsersAndManagersCannotReadOrSendAndAdminCannotReadBranchChat() {
        Long id=conversation();
        for(User actor:new User[]{outsider,foreignManager,admin}) {
            assertThatThrownBy(()->chat.history(actor.getUsername(),id,null)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(()->chat.send(actor.getUsername(),id,"Đọc trộm",key())).isInstanceOf(AccessDeniedException.class);
        }
        assertThat(messages.count()).isZero();
    }
    @Test void supportConversationIsPrivateBetweenCustomerAndAdmins() {
        Long id=chat.open(customer.getUsername(),"ADMIN",null).id();
        chat.send(admin.getUsername(),id,"Admin hỗ trợ bạn",key());
        assertThat(chat.history(customer.getUsername(),id,null).messages()).hasSize(1);
        assertThatThrownBy(()->chat.history(manager.getUsername(),id,null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->chat.open(admin.getUsername(),"STORE",store.getId())).isInstanceOf(AccessDeniedException.class);
    }
    @Test void blankOversizedAndInvalidRetryIdsAreRejected() {
        Long id=conversation();
        for(String body:new String[]{"   ","x".repeat(2001),"bad\0message"})
            assertThatThrownBy(()->chat.send(customer.getUsername(),id,body,key())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->chat.send(customer.getUsername(),id,"Tin nhắn","wrong-id")).isInstanceOf(IllegalArgumentException.class);
        assertThat(messages.count()).isZero();
    }
    @Test void retriesDoNotDuplicateUnicodeMessagesOrAcceptChangedContent() {
        Long id=conversation();String key=key();
        var first=chat.send(customer.getUsername(),id,"Hoa hồng đẹp quá 🌷",key);
        assertThat(chat.send(customer.getUsername(),id,"Hoa hồng đẹp quá 🌷",key).id()).isEqualTo(first.id());
        assertThatThrownBy(()->chat.send(customer.getUsername(),id,"Nội dung khác",key)).isInstanceOf(IllegalArgumentException.class);
        assertThat(messages.count()).isEqualTo(1);
    }
    @Test void disabledUsersCannotUseOldChatSessions() {
        Long id=conversation();customer.setIsActive(false);users.saveAndFlush(customer);
        assertThatThrownBy(()->chat.history(customer.getUsername(),id,null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->chat.send(customer.getUsername(),id,"Hi",key())).isInstanceOf(AccessDeniedException.class);
    }
    @Test void revokedMembershipIsNotBypassedByLegacyStoreAssignment() {
        foreignManager.setStore(store);users.save(foreignManager);
        var member=members.save(StoreMembership.builder().store(store).user(foreignManager).accessRole("PARTNER").active(true).build());
        Long id=conversation();chat.send(foreignManager.getUsername(),id,"Xin chào",key());
        member.setActive(false);members.saveAndFlush(member);
        assertThatThrownBy(()->chat.history(foreignManager.getUsername(),id,null)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void managerInboxIncludesOwnAdminChatWithNullStore() {
        Long support=chat.open(manager.getUsername(),"ADMIN",null).id();
        Long branch=conversation();
        assertThat(chat.inbox(manager.getUsername(),0).conversations()).extracting(ChatService.ConversationView::id).contains(support,branch);
    }
    @Test void imageUsesCloudAndDuplicateRetryDoesNotUploadAgain() throws Exception {
        Long id=conversation();String key=key();
        var file=new MockMultipartFile("file","flower.png","image/png",new byte[]{1,2,3});
        when(cloud.uploadToCloud(any(),anyString())).thenReturn(new CloudinaryService.UploadResult("https://res.cloudinary.com/test/image/upload/chat.png","chat/image"));
        var result=chat.sendImage(customer.getUsername(),id,"",key,file);
        assertThat(result.imageUrl()).startsWith("https://res.cloudinary.com/");
        assertThat(chat.sendImage(customer.getUsername(),id,"",key,file).id()).isEqualTo(result.id());
        verify(cloud,times(1)).uploadToCloud(file,"flower_shop/chat/"+id);
        verify(cloud,never()).upload(any(),anyString());
        assertThat(messages.findById(result.id()).orElseThrow().getImagePublicId()).isEqualTo("chat/image");
    }
    @Test void unauthorizedImageIsRejectedBeforeUpload() {
        Long id=conversation();var file=new MockMultipartFile("file","flower.png","image/png",new byte[]{1});
        assertThatThrownBy(()->chat.sendImage(outsider.getUsername(),id,"",key(),file)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(cloud);
    }
    @Test void cloudFailureDoesNotCreateMessage() throws Exception {
        Long id=conversation();when(cloud.uploadToCloud(any(),anyString())).thenThrow(new java.io.IOException("Cloud unavailable"));
        assertThatThrownBy(()->chat.sendImage(customer.getUsername(),id,"",key(),new MockMultipartFile("file","x.png","image/png",new byte[]{1}))).isInstanceOf(IllegalStateException.class);
        assertThat(messages.count()).isZero();
    }
}
