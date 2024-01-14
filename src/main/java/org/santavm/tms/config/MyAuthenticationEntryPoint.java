package org.santavm.tms.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MyAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // attribute added in JwtAuthenticationFilter
        if (request.getAttribute("jwt_exception") != null) {
            response.getOutputStream().println("{ \"error\": \"" + request.getAttribute("jwt_exception") + "\" }");
        } else {
            response.getOutputStream().println("{ \"error\": \"" + authException.getMessage() + "\" }");
        }

    }
}
