package com.authservice.security;

import com.authservice.service.LoggingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RequestLoggingFilter extends OncePerRequestFilter {
    
    private final LoggingService loggingService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Log request details before processing
        String requestInfo = String.format("Request received: %s %s", 
                request.getMethod(), request.getRequestURI());
        
        loggingService.logInfo("RequestLogging", requestInfo);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Process the request
            filterChain.doFilter(request, response);
        } finally {
            // Log response details after processing
            long duration = System.currentTimeMillis() - startTime;
            
            String responseInfo = String.format("Request completed: %s %s - Status: %d - Duration: %dms", 
                    request.getMethod(), request.getRequestURI(), response.getStatus(), duration);
            
            loggingService.logInfo("RequestLogging", responseInfo);
        }
    }
}