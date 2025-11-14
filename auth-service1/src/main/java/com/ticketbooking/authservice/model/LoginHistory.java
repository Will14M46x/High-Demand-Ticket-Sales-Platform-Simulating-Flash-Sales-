package com.ticketbooking.authservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_history", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_logged_in_at", columnList = "loggedInAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String ipAddress;
    
    @Column(length = 1000)
    private String userAgent;
    
    @Column
    private String deviceType; // Desktop, Mobile, Tablet
    
    @Column
    private String browser; // Chrome, Firefox, Safari, etc.
    
    @Column
    private String operatingSystem; // Windows, MacOS, Linux, iOS, Android
    
    @Column
    private String city;
    
    @Column
    private String country;
    
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime loggedInAt;
    
    @Column
    private LocalDateTime loggedOutAt;
    
    @Column(nullable = false)
    private Boolean suspicious = false;
    
    @Column(length = 500)
    private String suspiciousReason;
}

