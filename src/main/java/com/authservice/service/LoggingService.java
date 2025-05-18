package com.authservice.service;

import com.authservice.model.SystemLog;

import java.time.LocalDateTime;
import java.util.List;

public interface LoggingService {
    public void logInfo(String requestLogging, String requestInfo);

    public List<SystemLog> getLogsByLevel(String level, LocalDateTime startDate, LocalDateTime endDate);

    public List<SystemLog> getLogsByUsername(String username, LocalDateTime startDate, LocalDateTime endDate);
}
