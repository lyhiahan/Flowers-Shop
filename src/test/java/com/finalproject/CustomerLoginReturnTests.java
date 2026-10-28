package com.finalproject;

import com.finalproject.config.CustomAuthenticationSuccessHandler;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import static org.assertj.core.api.Assertions.assertThat;

class CustomerLoginReturnTests {
    private final CustomAuthenticationSuccessHandler handler = new CustomAuthenticationSuccessHandler();

    @Test void returnsCustomerToProductAndConsumesTarget() throws Exception {
        var request = new MockHttpServletRequest();
        request.getSession().setAttribute(CustomAuthenticationSuccessHandler.CUSTOMER_RETURN_TO, "/product/24");
        var response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(request, response, auth("USER"));
        assertThat(response.getRedirectedUrl()).isEqualTo("/product/24");
        assertThat(request.getSession().getAttribute(CustomAuthenticationSuccessHandler.CUSTOMER_RETURN_TO)).isNull();
    }

    @Test void returnsToSavedCartRequestButKeepsStaffInTheirPortal() throws Exception {
        for (String role : new String[]{"USER", "ADMIN", "MANAGER", "STAFF"}) {
            var request = new MockHttpServletRequest("GET", "/cart");
            var response = new MockHttpServletResponse();
            var cache = new HttpSessionRequestCache();
            cache.saveRequest(request, response);
            handler.onAuthenticationSuccess(request, response, auth(role));
            assertThat(response.getRedirectedUrl()).isEqualTo(switch (role) {
                case "ADMIN" -> "/admin/dashboard";
                case "MANAGER" -> "/manager/dashboard";
                case "STAFF" -> "/staff/orders";
                default -> "/cart";
            });
            assertThat(cache.getRequest(request, response)).isNull();
        }
    }

    @Test void rejectsExternalAndUnexpectedRedirectTargets() {
        for (String target : new String[]{"https://example.com", "//example.com", "/\\example.com", "/admin/dashboard",
                "/product/24/../logout", "/product/24#fragment", "/product/24\r\nLocation: https://example.com"}) {
            assertThat(CustomAuthenticationSuccessHandler.safeCustomerTarget(target)).as(target).isNull();
        }
        assertThat(CustomAuthenticationSuccessHandler.safeCustomerTarget("/products?categoryId=7")).isEqualTo("/products?categoryId=7");
    }

    private UsernamePasswordAuthenticationToken auth(String role) {
        return new UsernamePasswordAuthenticationToken("customer", "unused", AuthorityUtils.createAuthorityList("ROLE_" + role));
    }
}
