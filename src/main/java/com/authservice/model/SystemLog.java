package com.authservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column(nullable = false)
    private String level;
    
    @Column(nullable = false)
    private String logger;
    
    @Column(nullable = false, length = 500)
    private String message;
    
    @Column(length = 4000)
    private String stackTrace;
    
    @Column(length = 100)
    private String username;
    
    @Column(length = 100)
    private String ipAddress;
    
    @Column(length = 255)
    private String requestUrl;
    
    @Column(length = 10)
    private String requestMethod;
}