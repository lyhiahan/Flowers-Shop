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
        registry.setPreserveReceiveOrder(true);
        // Endpoint cho client kết nối SockJS
        registry.addEndpoint("/ws")
                .withSockJS();
        registry.addEndpoint("/ws-chat"); // Native WebSocket; default same-origin policy and HTTP session.
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Topic broadcast: /topic/store/{storeId}/orders, /topic/product/{productId}/stock
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setPreservePublishOrder(true);
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
                if (command == org.springframework.messaging.simp.stomp.StompCommand.SEND
                        || command == org.springframework.messaging.simp.stomp.StompCommand.CONNECT) {
                    if (headers.getUser() == null) throw new org.springframework.security.access.AccessDeniedException("Vui lòng đăng nhập.");
                    var sender = users.findByUsername(headers.getUser().getName()).orElseThrow();
                    if (!Boolean.TRUE.equals(sender.getIsActive()) || Boolean.TRUE.equals(sender.getAccountDisabledByAdmin()) || sender.isRetiredDeliveryAccount()) {
                        throw new org.springframework.security.access.AccessDeniedException("Tài khoản đã bị khóa.");
                    }
                    if (command == org.springframework.messaging.simp.stomp.StompCommand.SEND
                            && !"/app/chat.send".equals(headers.getDestination())) {
                        throw new org.springframework.security.access.AccessDeniedException("Không được gửi thông báo giả.");
                    }
                }
                if (command == org.springframework.messaging.simp.stomp.StompCommand.SUBSCRIBE) {
                    String destination = headers.getDestination();
                    if (headers.getUser() == null || destination == null) {
                        throw new org.springframework.security.access.AccessDeniedException("Kênh thông báo không hợp lệ.");
                    }
                    var user = users.findByUsername(headers.getUser().getName()).orElseThrow();
                    if (!Boolean.TRUE.equals(user.getIsActive()) || Boolean.TRUE.equals(user.getAccountDisabledByAdmin()) || user.isRetiredDeliveryAccount()) {
                        throw new org.springframework.security.access.AccessDeniedException("Tài khoản chưa được kích hoạt.");
                    }
                    if ("/user/queue/chat".equals(destination) || "/user/queue/chat.errors".equals(destination)
                            || "/user/queue/chat.acks".equals(destination)) return message;
                    if (destination.matches("^/topic/store/[0-9]+/orders$")) {
                        Long storeId = Long.valueOf(destination.split("/")[3]);
                        var store = stores.findById(storeId).orElseThrow();
                        boolean roleAllowed = user.hasRole(com.finalproject.enums.Role.ADMIN)
                                || user.hasRole(com.finalproject.enums.Role.MANAGER);
                        boolean allowed = user.hasRole(com.finalproject.enums.Role.ADMIN)
                                || users.findChatManagers(storeId, com.finalproject.enums.Role.MANAGER).stream().anyMatch(m -> m.getId().equals(user.getId()));
                        if (!roleAllowed || !allowed) {
                            throw new org.springframework.security.access.AccessDeniedException("Không có quyền nhận thông báo chi nhánh này.");
                        }
                    } else if (destination.matches("^/topic/user/[0-9]+/notifications$")) {
                        Long targetUserId = Long.valueOf(destination.split("/")[3]);
                        if (!user.getId().equals(targetUserId) && !user.hasRole(com.finalproject.enums.Role.ADMIN)) {
                            throw new org.springframework.security.access.AccessDeniedException("Không có quyền nhận thông báo của tài khoản khác.");
                        }
                    } else {
                        throw new org.springframework.security.access.AccessDeniedException("Kênh thông báo không hợp lệ.");
                    }
                }
                return message;
            }
        });
    }

    @Override
    public void configureWebSocketTransport(org.springframework.web.socket.config.annotation.WebSocketTransportRegistration registry) {
        registry.setMessageSizeLimit(16 * 1024).setSendBufferSizeLimit(64 * 1024).setSendTimeLimit(10000);
    }
}
