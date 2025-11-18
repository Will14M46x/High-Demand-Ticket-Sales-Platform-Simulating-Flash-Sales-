package com.ticketbooking.authservice;

import com.ticketbooking.authservice.dto.*;
import com.ticketbooking.authservice.exception.InvalidCredentialsException;
import com.ticketbooking.authservice.exception.UserAlreadyExistsException;
import com.ticketbooking.authservice.model.RefreshToken;
import com.ticketbooking.authservice.model.User;
import com.ticketbooking.authservice.repository.UserRepository;
import com.ticketbooking.authservice.service.*;
import com.ticketbooking.authservice.util.JwtUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock private UserRepository userRepository;
    @Mock private FirebaseService firebaseService;
    @Mock private JwtUtil jwtUtil;
    @Mock private RateLimitService rateLimitService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private LoginHistoryService loginHistoryService;

    private SignupRequest signupRequest;
    private LoginRequest loginRequest;
    private User user;

    @BeforeEach
    void setup() {
        // Firebase disabled for test
        authService.firebaseEnabled = false;

        // Fix NullPointerExceptions in tests
        authService.jwtExpiration = 86400000L;

        signupRequest = new SignupRequest("test@example.com", "password123", "Mikey", null);
        loginRequest = new LoginRequest("test@example.com", "password123");

        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setName("Mikey");
        user.setFirebaseUid("mock-uid-123");
        user.setIsActive(true);
    }

    @Test
    void testSignupSuccess() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtUtil.generateToken(anyString(), anyLong(), anyString())).thenReturn("jwt-token");

        RefreshToken refresh = new RefreshToken();
        refresh.setToken("refresh-123");
        when(refreshTokenService.createRefreshToken(any(), any(), any())).thenReturn(refresh);

        AuthResponse response = authService.signup(signupRequest, "127.0.0.1", "JUnit");

        assertEquals("jwt-token", response.getToken());
        assertEquals("refresh-123", response.getRefreshToken());
    }

    @Test
    void testSignupUserAlreadyExists() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
        assertThrows(UserAlreadyExistsException.class, () ->
                authService.signup(signupRequest, "127.0.0.1", "JUnit")
        );
    }

    @Test
    void testLoginSuccess() {
        when(rateLimitService.isLockedOut("test@example.com")).thenReturn(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(anyString(), anyLong(), anyString())).thenReturn("jwt-token");

        RefreshToken refresh = new RefreshToken();
        refresh.setToken("refresh-123");
        when(refreshTokenService.createRefreshToken(any(), any(), any())).thenReturn(refresh);

        AuthResponse response = authService.login(loginRequest, "127.0.0.1", "JUnit");

        assertEquals("jwt-token", response.getToken());
    }

    @Test
    void testLoginInvalidCredentials() {
        when(rateLimitService.isLockedOut("test@example.com")).thenReturn(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // Mark lenient – Firebase won't be called when firebaseEnabled=false
        lenient().when(firebaseService.verifyPassword(anyString(), anyString()))
                .thenThrow(new RuntimeException("Invalid password"));

        assertThrows(InvalidCredentialsException.class, () ->
                authService.login(loginRequest, "127.0.0.1", "JUnit")
        );
    }

    @Test
    void testValidateToken() {
        when(jwtUtil.validateToken("abc")).thenReturn(true);
        assertTrue(authService.validateToken("abc"));
    }

    @Test
    void testRefreshAccessTokenSuccess() {
        RefreshToken old = new RefreshToken();
        old.setUser(user);
        old.setToken("old");
        old.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(refreshTokenService.findByToken("old")).thenReturn(Optional.of(old));
        when(refreshTokenService.verifyToken(old)).thenReturn(true);

        RefreshToken newRefresh = new RefreshToken();
        newRefresh.setToken("new-token");
        when(refreshTokenService.rotateToken(any(), any(), any())).thenReturn(newRefresh);

        when(jwtUtil.generateToken(anyString(), anyLong(), anyString())).thenReturn("jwt-token");

        AuthResponse response = authService.refreshAccessToken("old", "127.0.0.1", "JUnit");

        assertEquals("new-token", response.getRefreshToken());
    }

    @Test
    void testRefreshAccessTokenInvalid() {
        when(refreshTokenService.findByToken("bad")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () ->
                authService.refreshAccessToken("bad", "127.0.0.1", "JUnit")
        );
    }
}