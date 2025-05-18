package com.authservice.service.implementations;

import com.authservice.model.SystemLog;
import com.authservice.repository.SystemLogRepository;
import com.authservice.service.LoggingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoggingServiceImpl implements LoggingService {
    private static final Logger logger = LoggerFactory.getLogger(LoggingServiceImpl.class);
    
    private final SystemLogRepository systemLogRepository;

    @Override
    public void logInfo(String loggerName, String message) {
        log(loggerName, "INFO", message, null);
    }
    
    public void logWarning(String loggerName, String message) {
        log(loggerName, "WARN", message, null);
    }
    
    public void logError(String loggerName, String message, Throwable throwable) {
        log(loggerName, "ERROR", message, throwable);
    }
    
    private void log(String loggerName, String level, String message, Throwable throwable) {
        Logger specificLogger = LoggerFactory.getLogger(loggerName);
        
        // Log to standard logging system
        switch (level) {
            case "INFO":
                specificLogger.info(message);
                break;
            case "WARN":
                specificLogger.warn(message);
                break;
            case "ERROR":
                specificLogger.error(message, throwable);
                break;
        }
        
        // Also save to database
        try {
            // Get request context if available
            HttpServletRequest request = null;
            String username = null;
            String ipAddress = null;
            String requestUrl = null;
            String requestMethod = null;
            
            if (RequestContextHolder.getRequestAttributes() != null) {
                request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
                requestUrl = request.getRequestURI();
                requestMethod = request.getMethod();
                ipAddress = request.getRemoteAddr();
            }
            
            // Get current authenticated user if available
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                username = authentication.getName();
            }
            
            // Build stack trace string if available
            String stackTraceStr = null;
            if (throwable != null) {
                StringBuilder sb = new StringBuilder();
                for (StackTraceElement element : throwable.getStackTrace()) {
                    sb.append(element.toString()).append("\n");
                    if (sb.length() > 3900) { // Stay under the 4000 char column limit
                        sb.append("... (truncated)");
                        break;
                    }
                }
                stackTraceStr = sb.toString();
            }
            
            // Create and save log entry
            SystemLog logEntry = SystemLog.builder()
                    .timestamp(LocalDateTime.now())
                    .level(level)
                    .logger(loggerName)
                    .message(message)
                    .stackTrace(stackTraceStr)
                    .username(username)
                    .ipAddress(ipAddress)
                    .requestUrl(requestUrl)
                    .requestMethod(requestMethod)
                    .build();
            
            systemLogRepository.save(logEntry);
        } catch (Exception e) {
            // If we can't save to database, at least log the failure
            logger.error("Failed to save log entry to database", e);
        }
    }
    
    @Override
    public List<SystemLog> getLogsByLevel(String level, LocalDateTime startDate, LocalDateTime endDate) {
        return systemLogRepository.findByLevelAndTimestampBetween(level, startDate, endDate);
    }

    @Override
    public List<SystemLog> getLogsByUsername(String username, LocalDateTime startDate, LocalDateTime endDate) {
        return systemLogRepository.findByUsernameAndTimestampBetween(username, startDate, endDate);
    }
}