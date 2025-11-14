package com.ticketbooking.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateLimitInfo {
    
    private Boolean isLockedOut;
    private Integer remainingAttempts;
    private Integer maxAttempts;
    private Long lockoutRemainingSeconds;
    private String message;
}

