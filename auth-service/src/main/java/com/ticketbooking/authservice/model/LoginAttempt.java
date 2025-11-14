package com.ticketbooking.authservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_attempts", indexes = {
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_ip_address", columnList = "ipAddress"),
    @Index(name = "idx_attempted_at", columnList = "attemptedAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAttempt {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String ipAddress;
    
    @Column(nullable = false)
    private Boolean successful;
    
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime attemptedAt;
    
    @Column(length = 500)
    private String failureReason;
    
    @Column
    private String userAgent;
    
    @Column
    private String deviceInfo;
}

