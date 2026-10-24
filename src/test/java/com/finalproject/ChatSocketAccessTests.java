package com.finalproject;

import com.finalproject.config.WebSocketConfig;
import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.repository.StoreRepository;
import com.finalproject.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.security.access.AccessDeniedException;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatSocketAccessTests {
    UserRepository users;
    com.finalproject.service.StoreAccessService access;
    ChannelInterceptor interceptor;
    User user;
    @BeforeEach void setup() {
        users=mock(UserRepository.class);
        var registration=mock(ChannelRegistration.class);
        new WebSocketConfig(users,mock(StoreRepository.class),access=mock(com.finalproject.service.StoreAccessService.class)).configureClientInboundChannel(registration);
        var capture=org.mockito.ArgumentCaptor.forClass(ChannelInterceptor.class);
        verify(registration).interceptors(capture.capture());
        interceptor=capture.getValue();
        user=User.builder().id(71L).username("socket_user").role(Role.USER).isActive(true).build();
        when(users.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
    }
    org.springframework.messaging.Message<byte[]> frame(StompCommand command,String destination,boolean loggedIn) {
        var headers=StompHeaderAccessor.create(command);
        if(loggedIn) headers.setUser(()->user.getUsername());
        headers.setDestination(destination);
        return MessageBuilder.createMessage(new byte[0],headers.getMessageHeaders());
    }
    @Test void privateQueuesBelongToAuthenticatedSessionAndCannotTargetAnotherUser() {
        assertThat(interceptor.preSend(frame(StompCommand.SUBSCRIBE,"/user/queue/chat",true),null)).isNotNull();
        assertThatThrownBy(()->interceptor.preSend(frame(StompCommand.SUBSCRIBE,"/user/other/queue/chat",true),null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->interceptor.preSend(frame(StompCommand.SUBSCRIBE,"/user/queue/chat",false),null)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void forgedMessagesCannotBypassApplicationAuthorization() {
        assertThat(interceptor.preSend(frame(StompCommand.SEND,"/app/chat.send",true),null)).isNotNull();
        for(String target:new String[]{"/queue/chat","/topic/user/71/notifications","/user/queue/chat"})
            assertThatThrownBy(()->interceptor.preSend(frame(StompCommand.SEND,target,true),null)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void blockedAccountCannotReusePreviouslyAuthenticatedSocket() {
        user.setAccountDisabledByAdmin(true);
        for(StompCommand command:new StompCommand[]{StompCommand.CONNECT,StompCommand.SEND,StompCommand.SUBSCRIBE})
            assertThatThrownBy(()->interceptor.preSend(frame(command,"/app/chat.send",true),null)).isInstanceOf(AccessDeniedException.class);
    }
    org.springframework.messaging.Message<byte[]> managerFrame(StompCommand command,String destination,Long branch,String requested) {
        var headers=StompHeaderAccessor.create(command);headers.setUser(()->user.getUsername());headers.setDestination(destination);
        var attributes=new java.util.HashMap<String,Object>();if(branch!=null) attributes.put("chatStoreId",branch);
        headers.setSessionAttributes(attributes);if(requested!=null) headers.setNativeHeader("chat-store-id",requested);
        return MessageBuilder.createMessage(new byte[0],headers.getMessageHeaders());
    }
    @Test void managerSocketMustSubscribeToItsBoundBranchNotGenericOrForeignQueue() {
        user.setRole(Role.MANAGER);
        when(access.managesStore(user,1L)).thenReturn(true);
        when(access.managesStore(user,2L)).thenReturn(false);
        assertThatThrownBy(()->interceptor.preSend(managerFrame(StompCommand.CONNECT,null,null,"2"),null)).isInstanceOf(AccessDeniedException.class);
        var connect=managerFrame(StompCommand.CONNECT,null,null,"1");
        assertThat(interceptor.preSend(connect,null)).isNotNull();
        assertThat(org.springframework.messaging.simp.SimpMessageHeaderAccessor.getSessionAttributes(connect.getHeaders())).containsEntry("chatStoreId",1L);
        assertThat(interceptor.preSend(managerFrame(StompCommand.SUBSCRIBE,"/user/queue/chat.store.1",1L,null),null)).isNotNull();
        assertThat(interceptor.preSend(managerFrame(StompCommand.SUBSCRIBE,"/user/queue/chat.support.1",1L,null),null)).isNotNull();
        for(String target:java.util.List.of("/user/queue/chat","/user/queue/chat.store.2","/user/queue/chat.support.2"))
            assertThatThrownBy(()->interceptor.preSend(managerFrame(StompCommand.SUBSCRIBE,target,1L,null),null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->interceptor.preSend(managerFrame(StompCommand.SEND,"/app/chat.send",null,null),null)).isInstanceOf(AccessDeniedException.class);
        when(access.managesStore(user,1L)).thenReturn(false);
        assertThatThrownBy(()->interceptor.preSend(managerFrame(StompCommand.SEND,"/app/chat.send",1L,null),null)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void unauthorizedBranchCannotBeBoundAndCustomersCannotSubscribeToManagerQueues() {
        assertThatThrownBy(()->interceptor.preSend(managerFrame(StompCommand.SUBSCRIBE,"/user/queue/chat.store.1",1L,null),null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->interceptor.preSend(managerFrame(StompCommand.SUBSCRIBE,"/user/queue/chat.support.1",1L,null),null)).isInstanceOf(AccessDeniedException.class);
        user.setRole(Role.ADMIN);
        assertThatThrownBy(()->interceptor.preSend(managerFrame(StompCommand.SUBSCRIBE,"/user/queue/chat.store.1",1L,null),null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->interceptor.preSend(managerFrame(StompCommand.SUBSCRIBE,"/user/queue/chat.support.1",1L,null),null)).isInstanceOf(AccessDeniedException.class);
        user.setRole(Role.MANAGER);
        for(String target:java.util.List.of("1","-1","bad"))
            assertThatThrownBy(()->interceptor.preSend(managerFrame(StompCommand.CONNECT,null,null,target),null)).isInstanceOf(AccessDeniedException.class);
    }

    @Test void staffSocketUsesOnlyAssignedBranchCustomerQueueAndCannotReceiveInternalSupport() {
        user.setRole(Role.STAFF);
        when(access.managesStore(user,1L)).thenReturn(true);
        assertThat(interceptor.preSend(managerFrame(StompCommand.CONNECT,null,null,"1"),null)).isNotNull();
        assertThat(interceptor.preSend(managerFrame(StompCommand.SUBSCRIBE,"/user/queue/chat.store.1",1L,null),null)).isNotNull();
        for (String target : java.util.List.of("/user/queue/chat.support.1", "/user/queue/chat.store.2", "/user/queue/chat"))
            assertThatThrownBy(()->interceptor.preSend(managerFrame(StompCommand.SUBSCRIBE,target,1L,null),null))
                    .isInstanceOf(AccessDeniedException.class);
        when(access.managesStore(user,1L)).thenReturn(false);
        assertThatThrownBy(()->interceptor.preSend(managerFrame(StompCommand.SEND,"/app/chat.send",1L,null),null))
                .isInstanceOf(AccessDeniedException.class);
    }
}
