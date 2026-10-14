package com.finalproject.controller.customer;

import com.finalproject.service.DemoPaymentService;
import com.finalproject.service.DemoPaymentTunnel;
import com.google.zxing.*;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.*;
import java.security.Principal;
import java.util.*;

@Controller
@RequiredArgsConstructor
public class DemoPaymentController {
    private final DemoPaymentService payments;
    private final DemoPaymentTunnel tunnel;
    @Value("${app.demo-payment.base-url:}") private String configuredBaseUrl;

    @ModelAttribute
    void privateResponse(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("X-Robots-Tag", "noindex, nofollow");
    }

    @GetMapping("/customer/demo-payment/{token}")
    public String desktop(@PathVariable String token, Principal principal, Model model, HttpServletRequest request) {
        var payment = payments.view(token, principal.getName());
        if (payment.status() == DemoPaymentService.Status.SUCCESS)
            return "redirect:/customer/demo-payment/" + token + "/success";
        model.addAttribute("payment", payment);
        var connection = phoneConnection(request, token);
        model.addAttribute("phoneUrl", connection.url());
        model.addAttribute("phoneConnection", connection);
        model.addAttribute("desktop", true);
        return "customer/demo-payment";
    }

    @GetMapping("/demo-payment/{token}")
    public String phone(@PathVariable String token, Model model) {
        var payment = payments.view(token, null);
        if (payment.status() == DemoPaymentService.Status.SUCCESS)
            return "redirect:/demo-payment/" + token + "/success";
        model.addAttribute("payment", payment);
        model.addAttribute("desktop", false);
        return "customer/demo-payment";
    }

    @PostMapping("/demo-payment/{token}/confirm")
    public String confirm(@PathVariable String token) {
        var payment = payments.confirm(token);
        return "redirect:/demo-payment/" + token
                + (payment.status() == DemoPaymentService.Status.SUCCESS ? "/success" : "");
    }

    @GetMapping("/customer/demo-payment/{token}/success")
    public String desktopSuccess(@PathVariable String token, Principal principal, Model model) {
        var payment = payments.view(token, principal.getName());
        if (payment.status() != DemoPaymentService.Status.SUCCESS)
            return "redirect:/customer/demo-payment/" + token;
        model.addAttribute("payment", payment);
        model.addAttribute("desktop", true);
        model.addAttribute("ordersUrl", payment.orderIds().size() == 1
                ? "/customer/orders/" + payment.orderIds().get(0) : "/customer/orders");
        return "customer/demo-payment-success";
    }

    @GetMapping("/demo-payment/{token}/success")
    public String phoneSuccess(@PathVariable String token, Model model) {
        var payment = payments.view(token, null);
        if (payment.status() != DemoPaymentService.Status.SUCCESS)
            return "redirect:/demo-payment/" + token;
        model.addAttribute("payment", payment);
        model.addAttribute("desktop", false);
        return "customer/demo-payment-success";
    }

    @PostMapping("/customer/demo-payment/order/{id}")
    public String resume(@PathVariable Long id, Principal principal) {
        return "redirect:/customer/demo-payment/" + payments.create(List.of(id), principal.getName());
    }

    @GetMapping("/customer/demo-payment/{token}/status")
    @ResponseBody
    public DemoPaymentService.View status(@PathVariable String token, Principal principal) {
        return payments.view(token, principal.getName());
    }

    public record PhoneConnection(String state, String url, String message) {}

    @GetMapping("/customer/demo-payment/{token}/connection")
    @ResponseBody
    public PhoneConnection connection(@PathVariable String token, Principal principal, HttpServletRequest request) {
        payments.view(token, principal.getName());
        return phoneConnection(request, token);
    }

    private PhoneConnection phoneConnection(HttpServletRequest request, String token) {
        if (!tunnel.automatic()) return new PhoneConnection("READY", phoneUrl(request, token), "");
        var connection = tunnel.connection();
        return new PhoneConnection(connection.state(), connection.baseUrl() == null ? null
                : connection.baseUrl() + "/demo-payment/" + token, connection.message());
    }

    @GetMapping(value = "/customer/demo-payment/{token}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public byte[] qr(@PathVariable String token, Principal principal, HttpServletRequest request) throws Exception {
        payments.view(token, principal.getName());
        var matrix = new QRCodeWriter().encode(phoneUrl(request, token), BarcodeFormat.QR_CODE, 360, 360,
                Map.of(EncodeHintType.MARGIN, 4, EncodeHintType.CHARACTER_SET, "UTF-8"));
        BufferedImage image = new BufferedImage(360, 360, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 360; y++) for (int x = 0; x < 360; x++)
            image.setRGB(x, y, matrix.get(x, y) ? 0x111111 : 0xffffff);
        var output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private String phoneUrl(HttpServletRequest request, String token) {
        if (tunnel.automatic()) {
            var connection = phoneConnection(request, token);
            if (connection.url() == null)
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, connection.message());
            return connection.url();
        }
        String base = configuredBaseUrl.strip();
        if (base.isEmpty()) {
            String host = request.getServerName();
            if (Set.of("localhost", "127.0.0.1", "::1", "0:0:0:0:0:0:0:1").contains(host)) host = lanAddress();
            try {
                int port = request.getServerPort();
                base = new URI(request.getScheme(), null, host, port, request.getContextPath(), null, null).toASCIIString();
            } catch (URISyntaxException ex) { throw new IllegalStateException("Invalid payment address", ex); }
        }
        URI uri = URI.create(base);
        if (!Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Invalid DEMO_PAYMENT_BASE_URL");
        return base.replaceAll("/+$", "") + "/demo-payment/" + token;
    }

    private String lanAddress() {
        try {
            for (NetworkInterface network : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                String name = (network.getName() + " " + network.getDisplayName()).toLowerCase(Locale.ROOT);
                if (!network.isUp() || network.isLoopback() || network.isVirtual()
                        || name.contains("vmware") || name.contains("virtual") || name.contains("vethernet")) continue;
                for (InetAddress address : Collections.list(network.getInetAddresses()))
                    if (address instanceof Inet4Address && !address.isLoopbackAddress() && !address.isLinkLocalAddress())
                        return address.getHostAddress();
            }
        } catch (SocketException ignored) { }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Connect to Wi-Fi or set DEMO_PAYMENT_BASE_URL before creating the QR code");
    }
}
