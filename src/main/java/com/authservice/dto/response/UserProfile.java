package com.authservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfile {
    private Long id;
    private String name;
    private String username;
    private String email;
}