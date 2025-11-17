package com.ticketbooking.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    
    private String token;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long userId;
    private String email;
    private String name;
    private String firebaseUid;
    private Long expiresIn; // Seconds until token expires
    
    public AuthResponse(String token, Long userId, String email, String name, String firebaseUid) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.firebaseUid = firebaseUid;
        this.tokenType = "Bearer";
    }
}

