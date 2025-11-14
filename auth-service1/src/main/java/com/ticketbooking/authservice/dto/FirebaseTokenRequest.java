package com.ticketbooking.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FirebaseTokenRequest {
    
    @NotBlank(message = "Firebase token is required")
    private String firebaseToken;
}

