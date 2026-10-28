package com.finalproject;

import com.finalproject.entity.*;
import com.finalproject.enums.Role;
import com.finalproject.repository.*;
import com.finalproject.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:chat-session-tests;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ChatSessionIntegrationTests {
    @org.springframework.beans.factory.annotation.Value("${local.server.port}") int port;
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired ChatService chat;
    @Autowired PasswordEncoder passwords;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;

    User account(String name, Role role) {
        return users.saveAndFlush(User.builder().username(name).fullName(name).email(name + "@socket.test")
                .password(passwords.encode("socket-password")).role(role).isActive(true).build());
    }
    String base() { return "http://localhost:" + port; }
    CookieManager login(String username) throws Exception {
        var cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        var http = HttpClient.newBuilder().cookieHandler(cookies).build();
        var page = http.send(HttpRequest.newBuilder(URI.create(base() + "/login")).GET().build(), HttpResponse.BodyHandlers.ofString());
        var token = Pattern.compile("<input(?=[^>]*name=\"_csrf\")(?=[^>]*value=\"([^\"]+)\")[^>]*>").matcher(page.body());
        assertThat(token.find()).as("login form supplies CSRF token").isTrue();
        String body = "username=" + username + "&password=socket-password&_csrf=" + URLEncoder.encode(token.group(1), StandardCharsets.UTF_8);
        var response = http.send(HttpRequest.newBuilder(URI.create(base() + "/perform_login"))
                .header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("location").orElse("")).doesNotContain("error");
        var context = http.send(HttpRequest.newBuilder(URI.create(base() + "/api/chat/context")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(context.statusCode()).as(context.body()).isEqualTo(200);
        assertThat(context.body()).contains("\"userId\"");
        return cookies;
    }
    StompSession connect(WebSocketStompClient client, CookieManager cookies) throws Exception {
        return connect(client,cookies,null);
    }
    StompSession connect(WebSocketStompClient client, CookieManager cookies,Long storeId) throws Exception {
        var headers = new WebSocketHttpHeaders();
        headers.add("Cookie", String.join("; ", cookies.getCookieStore().getCookies().stream().map(c -> c.getName() + "=" + c.getValue()).toList()));
        headers.add("Origin", base());
        var connectHeaders=new StompHeaders();
        if(storeId!=null) connectHeaders.add("chat-store-id",storeId.toString());
        return client.connectAsync("ws://localhost:" + port + "/ws-chat", headers, connectHeaders,new StompSessionHandlerAdapter() {}).get(10, TimeUnit.SECONDS);
    }
    StompFrameHandler receiver(BlockingQueue<String> queue) {
        return new StompFrameHandler() {
            public Type getPayloadType(StompHeaders headers) { return byte[].class; }
            public void handleFrame(StompHeaders headers, Object payload) { queue.add(new String((byte[]) payload, StandardCharsets.UTF_8)); }
        };
    }
    @Test void authenticatedHttpSessionsConnectAndExchangeBranchAndAdminMessages() throws Exception {
        User customer = account("socket_customer", Role.USER);
        User manager = account("socket_manager", Role.MANAGER);
        account("socket_admin", Role.ADMIN);
        Store store = stores.saveAndFlush(Store.builder().name("Socket branch").address("Branch address").owner(manager).status(true).build());
        manager.setStore(store); users.saveAndFlush(manager);
        var customerCookies = login(customer.getUsername());
        var managerCookies = login(manager.getUsername());
        var adminCookies = login("socket_admin");
        var client = new WebSocketStompClient(new StandardWebSocketClient());
        var opened = new ArrayList<StompSession>();
        try {
            var customerSocket = connect(client, customerCookies); opened.add(customerSocket);
            var managerSocket = connect(client, managerCookies,store.getId()); opened.add(managerSocket);
            var adminSocket = connect(client, adminCookies); opened.add(adminSocket);
            var customerInbox = new LinkedBlockingQueue<String>();
            var managerInbox = new LinkedBlockingQueue<String>();
            var adminInbox = new LinkedBlockingQueue<String>();
            customerSocket.subscribe("/user/queue/chat", receiver(customerInbox));
            managerSocket.subscribe("/user/queue/chat.store."+store.getId(), receiver(managerInbox));
            adminSocket.subscribe("/user/queue/chat", receiver(adminInbox));
            // Subscription and SEND on the same connection must retain client frame order.
            long branch = chat.open(customer.getUsername(), "STORE", store.getId()).id();
            send(customerSocket, branch, "Xin chào chi nhánh 🌷");
            assertThat(customerInbox.poll(10, TimeUnit.SECONDS)).contains("Xin chào chi nhánh");
            assertThat(managerInbox.poll(10, TimeUnit.SECONDS)).contains("Xin chào chi nhánh");
            send(managerSocket, branch, "Chi nhánh trả lời");
            assertThat(customerInbox.poll(10, TimeUnit.SECONDS)).contains("Chi nhánh trả lời");
            assertThat(managerInbox.poll(10, TimeUnit.SECONDS)).contains("Chi nhánh trả lời");
            // Each branch's designated manager receives only that branch's messages.
            User secondManager=account("socket_manager_second",Role.MANAGER);
            Store secondStore=stores.saveAndFlush(Store.builder().name("Second socket branch").address("Second address").owner(secondManager).status(true).build());
            secondManager.setStore(secondStore);users.saveAndFlush(secondManager);
            var secondSocket=connect(client,login(secondManager.getUsername()),secondStore.getId());opened.add(secondSocket);
            var secondInbox=new LinkedBlockingQueue<String>();var secondErrors=new LinkedBlockingQueue<String>();
            secondSocket.subscribe("/user/queue/chat.store."+secondStore.getId(),receiver(secondInbox));
            secondSocket.subscribe("/user/queue/chat.errors",receiver(secondErrors));
            // Same-connection SUBSCRIBE then SEND ordering establishes that the
            // subscription is ready before another connection sends into this branch.
            send(secondSocket,branch,"Không được trả lời nhầm chi nhánh");
            assertThat(secondErrors.poll(10,TimeUnit.SECONDS)).contains("không thuộc hộp thư");
            long secondThread=chat.open(customer.getUsername(),"STORE",secondStore.getId()).id();
            send(customerSocket,secondThread,"Chỉ gửi chi nhánh thứ hai");
            assertThat(customerInbox.poll(10,TimeUnit.SECONDS)).contains("Chỉ gửi chi nhánh thứ hai");
            assertThat(secondInbox.poll(10,TimeUnit.SECONDS)).contains("Chỉ gửi chi nhánh thứ hai");
            assertThat(managerInbox.poll(500,TimeUnit.MILLISECONDS)).isNull();
            assertThat(chat.history(customer.getUsername(),branch,null).messages()).hasSize(2);
            long support = chat.open(customer.getUsername(), "ADMIN", null).id();
            send(customerSocket, support, "Admin hỗ trợ");
            assertThat(customerInbox.poll(10, TimeUnit.SECONDS)).contains("Admin hỗ trợ");
            assertThat(adminInbox.poll(10, TimeUnit.SECONDS)).contains("Admin hỗ trợ");
            send(adminSocket, support, "Admin trả lời");
            assertThat(customerInbox.poll(10, TimeUnit.SECONDS)).contains("Admin trả lời");
            assertThat(adminInbox.poll(10, TimeUnit.SECONDS)).contains("Admin trả lời");
            var internalInbox=new LinkedBlockingQueue<String>();
            managerSocket.subscribe("/user/queue/chat.support."+store.getId(),receiver(internalInbox));
            long internal=chat.open(manager.getUsername(),"BRANCH_SUPPORT",store.getId()).id();
            send(managerSocket,internal,"Chi nhánh cần hỗ trợ vận hành");
            assertThat(internalInbox.poll(10,TimeUnit.SECONDS)).contains("hỗ trợ vận hành");
            assertThat(adminInbox.poll(10,TimeUnit.SECONDS)).contains("hỗ trợ vận hành");
            send(adminSocket,internal,"Admin hỗ trợ nội bộ");
            assertThat(internalInbox.poll(10,TimeUnit.SECONDS)).contains("hỗ trợ nội bộ");
            assertThat(adminInbox.poll(10,TimeUnit.SECONDS)).contains("hỗ trợ nội bộ");
            assertThat(customerInbox.poll(500,TimeUnit.MILLISECONDS)).isNull();
            assertThat(secondInbox.poll(500,TimeUnit.MILLISECONDS)).isNull();
            var adminErrors=new LinkedBlockingQueue<String>();
            adminSocket.subscribe("/user/queue/chat.errors",receiver(adminErrors));
            send(adminSocket,branch,"Không được giả danh cửa hàng");
            assertThat(adminErrors.poll(10,TimeUnit.SECONDS)).contains("chỉ được giám sát");
            assertThat(managerInbox.poll(500,TimeUnit.MILLISECONDS)).isNull();
            assertThat(customerInbox.poll(500,TimeUnit.MILLISECONDS)).isNull();
            assertThat(chat.history(customer.getUsername(), branch, null).messages()).hasSize(2);
            var category=categories.saveAndFlush(Category.builder().name("Socket product context").build());
            var product=products.saveAndFlush(Product.builder().name("Lan hỏi qua WebSocket").store(store).category(category)
                .imageUrl("https://example.test/orchid.jpg").priceBuy(java.math.BigDecimal.TEN).importPrice(java.math.BigDecimal.ONE)
                .build());
            send(customerSocket,branch,"Tôi hỏi mẫu lan này",product.getId());
            assertThat(customerInbox.poll(10,TimeUnit.SECONDS)).contains("\"product\":", "Lan hỏi qua WebSocket", "orchid.jpg");
            assertThat(managerInbox.poll(10,TimeUnit.SECONDS)).contains("\"product\":", "Lan hỏi qua WebSocket");
            var saved=chat.history(manager.getUsername(),branch,null,store.getId()).messages().getLast();
            assertThat(saved.product().id()).isEqualTo(product.getId());
        } finally {
            opened.forEach(StompSession::disconnect); client.stop();
        }
    }
    void send(StompSession session, long conversation, String content) {
        send(session,conversation,content,null);
    }
    void send(StompSession session, long conversation, String content,Long productId) {
        var headers = new StompHeaders(); headers.setDestination("/app/chat.send");
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        session.send(headers, ("{\"conversationId\":" + conversation + ",\"content\":\"" + content + "\",\"clientMessageId\":\"" + UUID.randomUUID() + "\",\"productId\":"+productId+"}").getBytes(StandardCharsets.UTF_8));
    }
}
