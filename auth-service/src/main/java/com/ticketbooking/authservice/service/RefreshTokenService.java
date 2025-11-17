package com.ticketbooking.authservice.service;

import com.ticketbooking.authservice.model.RefreshToken;
import com.ticketbooking.authservice.model.User;
import com.ticketbooking.authservice.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {
    
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final int MAX_ACTIVE_TOKENS_PER_USER = 5;
    
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    
    @Value("${refresh-token.expiration-days:30}")
    private int expirationDays;
    
    /**
     * Create a new refresh token for a user
     */
    @Transactional
    public RefreshToken createRefreshToken(User user, String ipAddress, String deviceInfo) {
        // Check if user has too many active tokens
        long activeTokenCount = refreshTokenRepository.countActiveTokensByUser(user, LocalDateTime.now());
        
        if (activeTokenCount >= MAX_ACTIVE_TOKENS_PER_USER) {
            // Revoke oldest token
            List<RefreshToken> userTokens = refreshTokenRepository.findByUserAndIsRevokedFalse(user);
            if (!userTokens.isEmpty()) {
                RefreshToken oldestToken = userTokens.get(0); // Assuming ordered by creation
                revokeToken(oldestToken.getToken());
                logger.info("Revoked oldest refresh token for user {} due to max limit", user.getEmail());
            }
        }
        
        // Generate unique token
        String tokenValue = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        
        RefreshToken refreshToken = RefreshToken.builder()
            .token(tokenValue)
            .user(user)
            .expiresAt(LocalDateTime.now().plusDays(expirationDays))
            .isRevoked(false)
            .ipAddress(ipAddress)
            .deviceInfo(deviceInfo)
            .build();
        
        refreshToken = refreshTokenRepository.save(refreshToken);
        logger.info("Created refresh token for user: {} (expires in {} days)", user.getEmail(), expirationDays);
        
        return refreshToken;
    }
    
    /**
     * Find and validate a refresh token
     */
    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
    
    /**
     * Verify if a token is valid
     */
    @Transactional(readOnly = true)
    public boolean verifyToken(RefreshToken token) {
        if (token == null) {
            return false;
        }
        
        if (token.getIsRevoked()) {
            logger.warn("Attempted to use revoked refresh token");
            return false;
        }
        
        if (token.isExpired()) {
            logger.warn("Attempted to use expired refresh token");
            return false;
        }
        
        return true;
    }
    
    /**
     * Revoke a specific refresh token
     */
    @Transactional
    public void revokeToken(String token) {
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByToken(token);
        
        if (refreshToken.isPresent()) {
            RefreshToken rt = refreshToken.get();
            rt.setIsRevoked(true);
            rt.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(rt);
            logger.info("Revoked refresh token for user: {}", rt.getUser().getEmail());
        }
    }
    
    /**
     * Revoke all refresh tokens for a user (e.g., on password change)
     */
    @Transactional
    public void revokeAllUserTokens(User user) {
        int revokedCount = refreshTokenRepository.revokeAllUserTokens(user, LocalDateTime.now());
        logger.info("Revoked {} refresh tokens for user: {}", revokedCount, user.getEmail());
    }
    
    /**
     * Get all active refresh tokens for a user
     */
    @Transactional(readOnly = true)
    public List<RefreshToken> getActiveUserTokens(User user) {
        return refreshTokenRepository.findByUserAndIsRevokedFalse(user);
    }
    
    /**
     * Get all refresh tokens (active and revoked) for a user
     */
    @Transactional(readOnly = true)
    public List<RefreshToken> getAllUserTokens(User user) {
        return refreshTokenRepository.findByUser(user);
    }
    
    /**
     * Scheduled task to clean up expired and revoked tokens
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "${refresh-token.cleanup-cron:0 0 2 * * ?}")
    @Transactional
    public void cleanupExpiredTokens() {
        logger.info("Starting cleanup of expired and revoked refresh tokens");
        int deletedCount = refreshTokenRepository.deleteExpiredAndRevokedTokens(LocalDateTime.now());
        logger.info("Cleaned up {} expired/revoked refresh tokens", deletedCount);
    }
    
    /**
     * Rotate a refresh token (create new one and revoke old one)
     */
    @Transactional
    public RefreshToken rotateToken(String oldToken, String ipAddress, String deviceInfo) {
        Optional<RefreshToken> oldRefreshToken = refreshTokenRepository.findByToken(oldToken);
        
        if (oldRefreshToken.isEmpty() || !verifyToken(oldRefreshToken.get())) {
            logger.warn("Attempted to rotate invalid refresh token");
            return null;
        }
        
        RefreshToken oldRt = oldRefreshToken.get();
        User user = oldRt.getUser();
        
        // Revoke old token
        revokeToken(oldToken);
        
        // Create new token
        RefreshToken newToken = createRefreshToken(user, ipAddress, deviceInfo);
        logger.info("Rotated refresh token for user: {}", user.getEmail());
        
        return newToken;
    }
}

