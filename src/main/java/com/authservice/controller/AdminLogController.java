package com.authservice.controller;

import com.authservice.model.SystemLog;
import com.authservice.service.LoggingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/logs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminLogController {

    private final LoggingService loggingService;
    
    @GetMapping("/level/{level}")
    public List<SystemLog> getLogsByLevel(
            @PathVariable String level,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return loggingService.getLogsByLevel(level, startDate, endDate);
    }
    
    @GetMapping("/user/{username}")
    public List<SystemLog> getLogsByUsername(
            @PathVariable String username,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return loggingService.getLogsByUsername(username, startDate, endDate);
    }
}