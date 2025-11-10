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
    
    @Value("${firebase.enabled:true}")
    private boolean firebaseEnabled;
    
    @Transactional
    public AuthResponse signup(SignupRequest request) {
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
            
            return new AuthResponse(token, user.getId(), user.getEmail(), user.getName(), user.getFirebaseUid());
            
        } catch (FirebaseAuthException e) {
            logger.error("Firebase authentication error during signup: {}", e.getMessage());
            throw new RuntimeException("Failed to create user: " + e.getMessage());
        }
    }
    
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        logger.info("Login request received for email: {}", request.getEmail());
        
        // Find user in database
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new UserNotFoundException("User not found with email: " + request.getEmail()));
        
        if (!user.getIsActive()) {
            throw new InvalidCredentialsException("User account is disabled");
        }
        
        try {
            if (firebaseEnabled) {
                // Verify with Firebase (in real scenario, client would send Firebase token)
                // For now, we verify user exists in Firebase
                UserRecord firebaseUser = firebaseService.getUserByEmail(request.getEmail());
                
                if (firebaseUser == null) {
                    throw new InvalidCredentialsException("Invalid credentials");
                }
            }
            
            // Generate JWT token
            String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getFirebaseUid());
            
            logger.info("User logged in successfully: {}", user.getEmail());
            return new AuthResponse(token, user.getId(), user.getEmail(), user.getName(), user.getFirebaseUid());
            
        } catch (FirebaseAuthException e) {
            logger.error("Firebase authentication error during login: {}", e.getMessage());
            throw new InvalidCredentialsException("Invalid credentials");
        }
    }
    
    @Transactional(readOnly = true)
    public AuthResponse verifyFirebaseToken(FirebaseTokenRequest request) {
        logger.info("Firebase token verification requested");
        
        try {
            FirebaseToken decodedToken = firebaseService.verifyToken(request.getFirebaseToken());
            
            if (decodedToken == null && !firebaseEnabled) {
                throw new InvalidCredentialsException("Firebase is disabled");
            }
            
            String email = decodedToken.getEmail();
            String firebaseUid = decodedToken.getUid();
            
            // Find or create user in database
            User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setName(decodedToken.getName());
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
}

