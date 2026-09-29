package com.finalproject.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@lombok.RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final com.finalproject.repository.UserRepository users;
    private final com.finalproject.repository.StoreRepository stores;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint cho client kết nối SockJS
        registry.addEndpoint("/ws")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Topic broadcast: /topic/store/{storeId}/orders, /topic/product/{productId}/stock
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(org.springframework.messaging.simp.config.ChannelRegistration registration) {
        registration.interceptors(new org.springframework.messaging.support.ChannelInterceptor() {
            @Override
            public org.springframework.messaging.Message<?> preSend(org.springframework.messaging.Message<?> message,
                    org.springframework.messaging.MessageChannel channel) {
                var headers = org.springframework.messaging.support.MessageHeaderAccessor.getAccessor(message,
                        org.springframework.messaging.simp.stomp.StompHeaderAccessor.class);
                if (headers == null) return message;
                var command = headers.getCommand();
                if (command == org.springframework.messaging.simp.stomp.StompCommand.SEND) {
                    throw new org.springframework.security.access.AccessDeniedException("Không được gửi thông báo giả.");
                }
                if (command == org.springframework.messaging.simp.stomp.StompCommand.SUBSCRIBE) {
                    String destination = headers.getDestination();
                    if (headers.getUser() == null || destination == null || !destination.matches("/topic/store/[0-9]+/orders")) {
                        throw new org.springframework.security.access.AccessDeniedException("Kênh thông báo không hợp lệ.");
                    }
                    var user = users.findByUsername(headers.getUser().getName()).orElseThrow();
                    Long storeId = Long.valueOf(destination.split("/")[3]);
                    var store = stores.findById(storeId).orElseThrow();
                    boolean roleAllowed = user.getRole() == com.finalproject.enums.Role.ADMIN
                            || user.getRole() == com.finalproject.enums.Role.MANAGER || user.getRole() == com.finalproject.enums.Role.VENDOR
                            || user.getRole() == com.finalproject.enums.Role.STAFF;
                    boolean allowed = user.getRole() == com.finalproject.enums.Role.ADMIN
                            || (user.getStore() != null && storeId.equals(user.getStore().getId()))
                            || (store.getOwner() != null && user.getId().equals(store.getOwner().getId()));
                    if (!Boolean.TRUE.equals(user.getIsActive()) || !roleAllowed || !allowed) {
                        throw new org.springframework.security.access.AccessDeniedException("Không có quyền nhận thông báo chi nhánh này.");
                    }
                }
                return message;
            }
        });
    }
}
