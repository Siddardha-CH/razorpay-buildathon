package com.recoup.backend.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Gates every money-relevant action (running a recovery batch) behind an API
 *  key. Read-only endpoints (cases, metrics) stay open for the dashboard. */
@Configuration
public class ApiKeyFilter {

    @Value("${app.api-key:dev-local-key}")
    private String expectedApiKey;

    @Bean
    public FilterRegistrationBean<HttpFilter> apiKeyFilterRegistration() {
        HttpFilter filter = new HttpFilter() {
            @Override
            protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                boolean mutating = "POST".equalsIgnoreCase(request.getMethod()) && request.getRequestURI().startsWith("/api/batches");
                if (mutating && !expectedApiKey.equals(request.getHeader("X-API-Key"))) {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"missing or invalid X-API-Key header\"}");
                    return;
                }
                chain.doFilter(request, response);
            }
        };
        FilterRegistrationBean<HttpFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/api/*");
        return registration;
    }
}
