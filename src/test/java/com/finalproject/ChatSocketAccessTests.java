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
    ChannelInterceptor interceptor;
    User user;
    @BeforeEach void setup() {
        users=mock(UserRepository.class);
        var registration=mock(ChannelRegistration.class);
        new WebSocketConfig(users,mock(StoreRepository.class)).configureClientInboundChannel(registration);
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
}
