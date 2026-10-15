package com.finalproject;

import com.finalproject.entity.*;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import com.finalproject.service.DemoPaymentService;
import com.finalproject.service.DemoPaymentTunnel;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.demo-payment.base-url=https://stale.trycloudflare.com")
@AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class DemoTunnelQrTests {
    @Autowired DemoPaymentService demo;
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired OrderRepository orders;
    @Autowired PaymentRepository payments;
    @Autowired MockMvc mvc;
    @Autowired Clock clock;
    @MockitoBean DemoPaymentTunnel tunnel;
    String token;

    @BeforeEach void setup() {
        User customer = users.save(User.builder().username("tunnel-customer").email("tunnel@test.local").password("unused")
                .fullName("Tunnel customer").role(Role.USER).isActive(true).build());
        Store store = stores.save(Store.builder().name("Tunnel store").address("Test address").build());
        Order order = orders.save(Order.builder().user(customer).pickupStore(store).orderType(OrderType.BUY)
                .status(OrderStatus.PENDING).paymentMethod("MOMO_DEMO").paymentStatus("UNPAID")
                .totalAmount(new BigDecimal("100000")).paymentExpiresAt(LocalDateTime.now(clock).plusMinutes(10)).build());
        payments.save(Payment.builder().order(order).amount(order.getTotalAmount()).method(PaymentMethod.MOMO_DEMO).status(PaymentStatus.PENDING).build());
        token = demo.create(List.of(order.getId()), customer.getUsername());
        when(tunnel.automatic()).thenReturn(true);
    }

    @Test void startupAndOutageRenderWithoutPublishingAStaleQr() throws Exception {
        for (String state : List.of("STARTING", "ERROR")) {
            when(tunnel.connection()).thenReturn(new DemoPaymentTunnel.Connection(state, null, "Đang bật kết nối điện thoại…"));
            var page = mvc.perform(get("/customer/demo-payment/" + token).with(user("tunnel-customer").roles("USER")))
                    .andExpect(status().isOk()).andReturn().getResponse();
            String html = page.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            assertThat(html).contains("Đang bật kết nối điện thoại", "id=\"payment-qr\"")
                    .doesNotContain("stale.trycloudflare.com", "src=\"/customer/demo-payment/" + token + "/qr\"");
            mvc.perform(get("/customer/demo-payment/" + token + "/qr").with(user("tunnel-customer").roles("USER")))
                    .andExpect(status().isServiceUnavailable());
            mvc.perform(get("/customer/demo-payment/" + token + "/connection").with(user("tunnel-customer").roles("USER")))
                    .andExpect(jsonPath("$.state").value(state)).andExpect(jsonPath("$.url").isEmpty());
            preview("starting.html", page.getContentAsByteArray());
        }
    }

    @Test void liveOriginChangesUpdateQrWithoutRestartingTheWebsite() throws Exception {
        for (String host : List.of("first-phone", "second-phone")) {
            when(tunnel.connection()).thenReturn(new DemoPaymentTunnel.Connection("READY", "https://" + host + ".trycloudflare.com", ""));
            String url = "https://" + host + ".trycloudflare.com/demo-payment/" + token;
            mvc.perform(get("/customer/demo-payment/" + token + "/connection").with(user("tunnel-customer").roles("USER")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.url").value(url));
            byte[] png = mvc.perform(get("/customer/demo-payment/" + token + "/qr").with(user("tunnel-customer").roles("USER")))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
            var image = ImageIO.read(new ByteArrayInputStream(png));
            var source = new RGBLuminanceSource(image.getWidth(), image.getHeight(), image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth()));
            // This is a pristine generated bitmap, not a camera frame needing finder detection.
            assertThat(new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(source)),
                    Map.of(DecodeHintType.PURE_BARCODE, true)).getText()).isEqualTo(url);
            var page = mvc.perform(get("/customer/demo-payment/" + token).with(user("tunnel-customer").roles("USER")))
                    .andExpect(status().isOk()).andReturn().getResponse();
            preview("ready.html", page.getContentAsByteArray()); preview("qr.png", png);
        }
    }

    @Test void connectionEndpointIsOwnerOnlyAndCannotBeExposedByTheGateway() throws Exception {
        mvc.perform(get("/customer/demo-payment/" + token + "/connection").with(user("someone-else").roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/customer/demo-payment/" + token + "/connection"))
                .andExpect(status().is3xxRedirection());
    }

    private void preview(String name, byte[] bytes) throws Exception {
        Path root = Path.of("target/auto-tunnel-preview"); Files.createDirectories(root); Files.write(root.resolve(name), bytes);
    }
}
