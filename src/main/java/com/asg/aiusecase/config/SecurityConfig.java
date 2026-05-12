package com.asg.aiusecase.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.security.enabled", havingValue = "true")
public class SecurityConfig extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_PREFIXES = Set.of("/actuator", "/swagger-ui", "/v3/api-docs");
    private final AppProperties properties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String configuredKey = properties.getSecurity().getApiKey();
        if (!properties.getSecurity().isEnabled()
                || configuredKey == null
                || configuredKey.isBlank()
                || isPublic(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }
        String providedKey = request.getHeader("X-API-Key");
        if (!configuredKey.equals(providedKey)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Invalid or missing API key\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isPublic(String uri) {
        return PUBLIC_PREFIXES.stream().anyMatch(uri::startsWith);
    }
}
