package com.ticketbooking.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User context extracted from JWT token
 * Contains authenticated user information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContext {
    private Long userId;
    private String email;
    private String firebaseUid;
    
    /**
     * Check if user context is valid
     */
    public boolean isValid() {
        return userId != null && email != null && !email.isEmpty();
    }
}

