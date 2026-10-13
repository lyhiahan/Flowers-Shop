package com.finalproject.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

/** Keep only the attempted identifier in this browser session; never retain the password. */
@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    public static final String ATTEMPTED_USERNAME = "LOGIN_ATTEMPTED_USERNAME";
    public static final int MAX_IDENTIFIER_LENGTH = 254;

    public CustomAuthenticationFailureHandler() {
        super("/login?error=true");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String submitted = request.getParameter("username");
        String identifier = submitted == null ? "" : submitted.substring(0, Math.min(submitted.length(), MAX_IDENTIFIER_LENGTH));
        request.getSession().setAttribute(ATTEMPTED_USERNAME, identifier);
        super.onAuthenticationFailure(request, response, exception);
    }
}
