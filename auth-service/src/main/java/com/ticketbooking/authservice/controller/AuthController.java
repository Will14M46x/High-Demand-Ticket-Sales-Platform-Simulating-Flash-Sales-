package com.ticketbooking.authservice.controller;

import com.ticketbooking.authservice.dto.*;
import com.ticketbooking.authservice.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private AuthService authService;
    
    /**
     * Extract IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
    
    /**
     * Extract User-Agent from request
     */
    private String getUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "Unknown";
    }
    
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request, HttpServletRequest httpRequest) {
        logger.info("POST /api/auth/signup - Email: {}", request.getEmail());
        String ipAddress = getClientIp(httpRequest);
        String userAgent = getUserAgent(httpRequest);
        AuthResponse response = authService.signup(request, ipAddress, userAgent);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        logger.info("POST /api/auth/login - Email: {}", request.getEmail());
        String ipAddress = getClientIp(httpRequest);
        String userAgent = getUserAgent(httpRequest);
        AuthResponse response = authService.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        logger.info("POST /api/auth/refresh-token");
        String ipAddress = getClientIp(httpRequest);
        String userAgent = getUserAgent(httpRequest);
        AuthResponse response = authService.refreshAccessToken(request.getRefreshToken(), ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        logger.info("POST /api/auth/logout");
        authService.logout(request.getRefreshToken());
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logged out successfully");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, String>> logoutAllDevices(@RequestHeader("Authorization") String authHeader) {
        logger.info("POST /api/auth/logout-all");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            // Extract userId from token - you'll need to implement this in JwtUtil
            // For now, we'll expect it in request body
            Map<String, String> response = new HashMap<>();
            response.put("message", "This endpoint requires userId - please use /logout-all/{userId}");
            return ResponseEntity.badRequest().body(response);
        }
        
        throw new IllegalArgumentException("Missing authorization header");
    }
    
    @PostMapping("/logout-all/{userId}")
    public ResponseEntity<Map<String, String>> logoutAllDevicesByUserId(@PathVariable Long userId) {
        logger.info("POST /api/auth/logout-all/{}", userId);
        authService.logoutAllDevices(userId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logged out from all devices successfully");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/rate-limit/{email}")
    public ResponseEntity<RateLimitInfo> getRateLimitInfo(@PathVariable String email) {
        logger.info("GET /api/auth/rate-limit/{}", email);
        RateLimitInfo info = authService.getRateLimitInfo(email);
        return ResponseEntity.ok(info);
    }
    
    @GetMapping("/login-history/{userId}")
    public ResponseEntity<List<LoginHistoryDto>> getLoginHistory(@PathVariable Long userId) {
        logger.info("GET /api/auth/login-history/{}", userId);
        List<LoginHistoryDto> history = authService.getUserLoginHistory(userId);
        return ResponseEntity.ok(history);
    }
    
    @GetMapping("/active-sessions/{userId}")
    public ResponseEntity<List<ActiveSessionDto>> getActiveSessions(@PathVariable Long userId) {
        logger.info("GET /api/auth/active-sessions/{}", userId);
        List<ActiveSessionDto> sessions = authService.getActiveSessions(userId);
        return ResponseEntity.ok(sessions);
    }
    
    @PostMapping("/verify-firebase-token")
    public ResponseEntity<AuthResponse> verifyFirebaseToken(@Valid @RequestBody FirebaseTokenRequest request) {
        logger.info("POST /api/auth/verify-firebase-token");
        AuthResponse response = authService.verifyFirebaseToken(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/validate-token")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestHeader("Authorization") String authHeader) {
        logger.info("GET /api/auth/validate-token");
        
        Map<String, Object> response = new HashMap<>();
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Boolean isValid = authService.validateToken(token);
            response.put("valid", isValid);
            return ResponseEntity.ok(response);
        }
        
        response.put("valid", false);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long userId) {
        logger.info("GET /api/auth/user/{}", userId);
        UserDto user = authService.getUserById(userId);
        return ResponseEntity.ok(user);
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "auth-service");
        return ResponseEntity.ok(response);
    }
}

