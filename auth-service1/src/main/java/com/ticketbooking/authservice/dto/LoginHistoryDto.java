package com.ticketbooking.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginHistoryDto {
    
    private Long id;
    private String ipAddress;
    private String deviceType;
    private String browser;
    private String operatingSystem;
    private String city;
    private String country;
    private LocalDateTime loggedInAt;
    private Boolean suspicious;
    private String suspiciousReason;
}

