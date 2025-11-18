package com.ticketbooking.authservice.service;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.ticketbooking.authservice.dto.*;
import com.ticketbooking.authservice.exception.InvalidCredentialsException;
import com.ticketbooking.authservice.exception.UserAlreadyExistsException;
import com.ticketbooking.authservice.exception.UserNotFoundException;
import com.ticketbooking.authservice.model.User;
import com.ticketbooking.authservice.repository.UserRepository;
import com.ticketbooking.authservice.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private FirebaseService firebaseService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private RateLimitService rateLimitService;
    
    @Autowired
    private RefreshTokenService refreshTokenService;
    
    @Autowired
    private LoginHistoryService loginHistoryService;
    
    @Value("${firebase.enabled:true}")
    public boolean firebaseEnabled;
    
    @Value("${jwt.expiration:86400000}")
    public Long jwtExpiration;
    
    @Transactional
    public AuthResponse signup(SignupRequest request, String ipAddress, String userAgent) {
        logger.info("Signup request received for email: {}", request.getEmail());
        
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + request.getEmail() + " already exists");
        }
        
        try {
            String firebaseUid;
            
            if (firebaseEnabled) {
                // Create user in Firebase
                UserRecord firebaseUser = firebaseService.createUser(
                    request.getEmail(),
                    request.getPassword(),
                    request.getName()
                );
                
                // Check if Firebase user creation was successful
                if (firebaseUser == null) {
                    throw new RuntimeException("Failed to create user in Firebase: Firebase returned null");
                }
                
                firebaseUid = firebaseUser.getUid();
            } else {
                // Mock Firebase UID for testing
                firebaseUid = "mock-uid-" + UUID.randomUUID().toString();
            }
            
            // Create user in our database
            User user = new User();
            user.setEmail(request.getEmail());
            user.setName(request.getName());
            user.setFirebaseUid(firebaseUid);
            user.setProvider("firebase");
            user.setPhoneNumber(request.getPhoneNumber());
            user.setIsActive(true);
            user.setIsEmailVerified(false);
            
            user = userRepository.save(user);
            logger.info("User created successfully with ID: {}", user.getId());
            
            // Generate JWT token
            String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getFirebaseUid());
            
            // Generate refresh token
            com.ticketbooking.authservice.model.RefreshToken refreshToken = 
                refreshTokenService.createRefreshToken(user, ipAddress, userAgent);
            
            // Record login history
            loginHistoryService.recordLogin(user, ipAddress, userAgent);
            
            // Record successful attempt in rate limiter
            rateLimitService.recordSuccessfulAttempt(user.getEmail(), ipAddress, userAgent);
            
            return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .firebaseUid(user.getFirebaseUid())
                .expiresIn(jwtExpiration / 1000) // Convert to seconds
                .build();
            
        } catch (FirebaseAuthException e) {
            logger.error("Firebase authentication error during signup: {}", e.getMessage());
            throw new RuntimeException("Failed to create user: " + e.getMessage());
        }
    }
    
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        logger.info("Login request received for email: {}", request.getEmail());
        
        // Check if account is locked due to failed attempts
        if (rateLimitService.isLockedOut(request.getEmail())) {
            Long remainingTime = rateLimitService.getRemainingLockoutTime(request.getEmail());
            int remainingMinutes = (int) (remainingTime / 60);
            logger.warn("Login attempt for locked account: {}", request.getEmail());
            throw new InvalidCredentialsException(
                String.format("Account locked due to multiple failed login attempts. Try again in %d minutes.", 
                    remainingMinutes));
        }
        
        // Find user in database
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> {
                // Record failed attempt even if user doesn't exist (to prevent user enumeration attacks)
                rateLimitService.recordFailedAttempt(request.getEmail(), ipAddress, userAgent, "User not found");
                return new UserNotFoundException("Invalid email or password");
            });
        
        if (!user.getIsActive()) {
            rateLimitService.recordFailedAttempt(request.getEmail(), ipAddress, userAgent, "Account disabled");
            throw new InvalidCredentialsException("User account is disabled");
        }
        
        try {
            // CRITICAL: Verify password with Firebase before issuing JWT token
            if (firebaseEnabled) {
                // Verify password using Firebase Authentication REST API
                String firebaseIdToken = firebaseService.verifyPassword(request.getEmail(), request.getPassword());
                
                if (firebaseIdToken == null) {
                    // This should only happen if Firebase is disabled
                    logger.warn("Firebase returned null token during password verification");
                    rateLimitService.recordFailedAttempt(request.getEmail(), ipAddress, userAgent, "Invalid password");
                    throw new InvalidCredentialsException("Invalid credentials");
                }
                
                logger.debug("Password verified successfully with Firebase for user: {}", request.getEmail());
            } else {
                // Firebase is disabled (dev/test mode)
                // WARNING: In production, this should NEVER be reached
                logger.warn("Firebase is disabled - password verification skipped for: {}", request.getEmail());
                logger.warn("This is INSECURE and should only be used in development/testing!");
            }
            
            // Generate JWT token only after successful password verification
            String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getFirebaseUid());
            
            // Generate refresh token
            com.ticketbooking.authservice.model.RefreshToken refreshToken = 
                refreshTokenService.createRefreshToken(user, ipAddress, userAgent);
            
            // Record successful login attempt
            rateLimitService.recordSuccessfulAttempt(user.getEmail(), ipAddress, userAgent);
            
            // Record login history
            loginHistoryService.recordLogin(user, ipAddress, userAgent);
            
            logger.info("User logged in successfully: {}", user.getEmail());
            
            return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .firebaseUid(user.getFirebaseUid())
                .expiresIn(jwtExpiration / 1000) // Convert to seconds
                .build();

        } catch (InvalidCredentialsException e) {
            // Re-throw credential exceptions
            throw e;
        } catch (Exception e) {
            logger.error("Authentication error during login for {}: {}", request.getEmail(), e.getMessage());
            // Record failed attempt
            rateLimitService.recordFailedAttempt(request.getEmail(), ipAddress, userAgent, 
                "Authentication error: " + e.getMessage());
            // Don't reveal whether it's email or password that's wrong - security best practice
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }
    
    @Transactional(readOnly = true)
    public AuthResponse verifyFirebaseToken(FirebaseTokenRequest request) {
        logger.info("Firebase token verification requested");
        
        try {
            FirebaseToken decodedToken = firebaseService.verifyToken(request.getFirebaseToken());
            
            // Check for null token before accessing its methods
            if (decodedToken == null) {
                if (!firebaseEnabled) {
                    throw new InvalidCredentialsException("Firebase is disabled");
                } else {
                    throw new InvalidCredentialsException("Invalid or expired Firebase token");
                }
            }
            
            String email = decodedToken.getEmail();
            String firebaseUid = decodedToken.getUid();
            
            // Validate required token fields
            if (email == null || email.trim().isEmpty()) {
                throw new InvalidCredentialsException("Firebase token does not contain email");
            }
            if (firebaseUid == null || firebaseUid.trim().isEmpty()) {
                throw new InvalidCredentialsException("Firebase token does not contain UID");
            }
            
            // Find or create user in database
            User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setName(decodedToken.getName() != null ? decodedToken.getName() : "Unknown");
                    newUser.setFirebaseUid(firebaseUid);
                    newUser.setProvider("firebase");
                    newUser.setIsActive(true);
                    newUser.setIsEmailVerified(decodedToken.isEmailVerified());
                    return userRepository.save(newUser);
                });
            
            // Generate JWT token
            String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getFirebaseUid());
            
            logger.info("Firebase token verified successfully for user: {}", user.getEmail());
            return new AuthResponse(token, user.getId(), user.getEmail(), user.getName(), user.getFirebaseUid());
            
        } catch (FirebaseAuthException e) {
            logger.error("Firebase token verification failed: {}", e.getMessage());
            throw new InvalidCredentialsException("Invalid Firebase token");
        }
    }
    
    @Transactional(readOnly = true)
    public UserDto getUserById(Long userId) {
        logger.info("Fetching user with ID: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        
        return UserDto.builder()
            .id(user.getId())
            .email(user.getEmail())
            .name(user.getName())
            .firebaseUid(user.getFirebaseUid())
            .provider(user.getProvider())
            .phoneNumber(user.getPhoneNumber())
            .isActive(user.getIsActive())
            .isEmailVerified(user.getIsEmailVerified())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
    }
    
    public Boolean validateToken(String token) {
        try {
            return jwtUtil.validateToken(token);
        } catch (Exception e) {
            logger.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Refresh access token using refresh token
     */
    @Transactional
    public AuthResponse refreshAccessToken(String refreshTokenValue, String ipAddress, String userAgent) {
        logger.info("Refresh token request received");
        
        // Find and validate refresh token
        Optional<com.ticketbooking.authservice.model.RefreshToken> refreshTokenOpt = 
            refreshTokenService.findByToken(refreshTokenValue);
        
        if (refreshTokenOpt.isEmpty()) {
            logger.warn("Refresh token not found");
            throw new InvalidCredentialsException("Invalid refresh token");
        }
        
        com.ticketbooking.authservice.model.RefreshToken refreshToken = refreshTokenOpt.get();
        
        if (!refreshTokenService.verifyToken(refreshToken)) {
            logger.warn("Refresh token validation failed");
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }
        
        User user = refreshToken.getUser();
        
        if (!user.getIsActive()) {
            logger.warn("Attempted to refresh token for inactive user: {}", user.getEmail());
            throw new InvalidCredentialsException("User account is disabled");
        }
        
        // Generate new JWT access token
        String newAccessToken = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getFirebaseUid());
        
        // Optionally rotate the refresh token (create new one, revoke old one)
        com.ticketbooking.authservice.model.RefreshToken newRefreshToken = 
            refreshTokenService.rotateToken(refreshTokenValue, ipAddress, userAgent);
        
        if (newRefreshToken == null) {
            logger.error("Failed to rotate refresh token");
            throw new RuntimeException("Failed to refresh token");
        }
        
        logger.info("Token refreshed successfully for user: {}", user.getEmail());
        
        return AuthResponse.builder()
            .token(newAccessToken)
            .refreshToken(newRefreshToken.getToken())
            .tokenType("Bearer")
            .userId(user.getId())
            .email(user.getEmail())
            .name(user.getName())
            .firebaseUid(user.getFirebaseUid())
            .expiresIn(jwtExpiration / 1000)
            .build();
    }
    
    /**
     * Logout - revoke refresh token
     */
    @Transactional
    public void logout(String refreshTokenValue) {
        logger.info("Logout request received");
        refreshTokenService.revokeToken(refreshTokenValue);
    }
    
    /**
     * Logout from all devices - revoke all refresh tokens for a user
     */
    @Transactional
    public void logoutAllDevices(Long userId) {
        logger.info("Logout all devices request for user ID: {}", userId);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        refreshTokenService.revokeAllUserTokens(user);
    }
    
    /**
     * Get rate limit info for an email
     */
    public RateLimitInfo getRateLimitInfo(String email) {
        boolean isLockedOut = rateLimitService.isLockedOut(email);
        int remainingAttempts = rateLimitService.getRemainingAttempts(email);
        long lockoutTime = isLockedOut ? rateLimitService.getRemainingLockoutTime(email) : 0L;
        
        String message;
        if (isLockedOut) {
            int minutes = (int) (lockoutTime / 60);
            message = String.format("Account locked. Try again in %d minutes.", minutes);
        } else if (remainingAttempts < rateLimitService.getMaxAttempts()) {
            message = String.format("%d attempts remaining before lockout", remainingAttempts);
        } else {
            message = "No failed attempts";
        }
        
        return RateLimitInfo.builder()
            .isLockedOut(isLockedOut)
            .remainingAttempts(remainingAttempts)
            .maxAttempts(rateLimitService.getMaxAttempts())
            .lockoutRemainingSeconds(lockoutTime)
            .message(message)
            .build();
    }
    
    /**
     * Get login history for a user
     */
    @Transactional(readOnly = true)
    public List<LoginHistoryDto> getUserLoginHistory(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        List<com.ticketbooking.authservice.model.LoginHistory> history = 
            loginHistoryService.getRecentLogins(user);
        
        return history.stream()
            .map(lh -> LoginHistoryDto.builder()
                .id(lh.getId())
                .ipAddress(lh.getIpAddress())
                .deviceType(lh.getDeviceType())
                .browser(lh.getBrowser())
                .operatingSystem(lh.getOperatingSystem())
                .city(lh.getCity())
                .country(lh.getCountry())
                .loggedInAt(lh.getLoggedInAt())
                .suspicious(lh.getSuspicious())
                .suspiciousReason(lh.getSuspiciousReason())
                .build())
            .toList();
    }
    
    /**
     * Get active sessions (refresh tokens) for a user
     */
    @Transactional(readOnly = true)
    public List<ActiveSessionDto> getActiveSessions(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        List<com.ticketbooking.authservice.model.RefreshToken> tokens = 
            refreshTokenService.getActiveUserTokens(user);
        
        return tokens.stream()
            .map(token -> ActiveSessionDto.builder()
                .id(token.getId())
                .deviceInfo(token.getDeviceInfo())
                .ipAddress(token.getIpAddress())
                .createdAt(token.getCreatedAt())
                .expiresAt(token.getExpiresAt())
                .isCurrent(false) // We can't easily determine this here
                .build())
            .toList();
    }
}

