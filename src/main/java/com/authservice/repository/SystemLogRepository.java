package com.authservice.repository;

import com.authservice.model.SystemLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {
    List<SystemLog> findByLevelAndTimestampBetween(String level, LocalDateTime start, LocalDateTime end);
    List<SystemLog> findByUsernameAndTimestampBetween(String username, LocalDateTime start, LocalDateTime end);
}