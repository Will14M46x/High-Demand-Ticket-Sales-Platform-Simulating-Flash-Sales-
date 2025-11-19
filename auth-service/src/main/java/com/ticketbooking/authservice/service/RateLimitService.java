package com.ticketbooking.authservice.service;

import com.ticketbooking.authservice.model.LoginAttempt;
import com.ticketbooking.authservice.repository.LoginAttemptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
public class RateLimitService {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);
    private static final String LOCKOUT_PREFIX = "lockout:";
    private static final String ATTEMPT_PREFIX = "attempt:";
    
    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private LoginAttemptRepository loginAttemptRepository;
    
    @Value("${rate-limit.enabled:true}")
    private boolean rateLimitEnabled;
    
    @Value("${rate-limit.max-attempts:5}")
    private int maxAttempts;
    
    @Value("${rate-limit.lockout-duration-minutes:15}")
    private int lockoutDurationMinutes;
    
    @Value("${rate-limit.attempt-window-minutes:30}")
    private int attemptWindowMinutes;
    
    /**
     * Check if an email is currently locked out
     */
    public boolean isLockedOut(String email) {
        if (!rateLimitEnabled || redisTemplate == null) {
            return false;
        }
        
        String lockoutKey = LOCKOUT_PREFIX + email;
        Boolean exists = redisTemplate.hasKey(lockoutKey);
        return Boolean.TRUE.equals(exists);
    }
    
    /**
     * Get remaining lockout time in seconds
     */
    public Long getRemainingLockoutTime(String email) {
        if (!rateLimitEnabled || redisTemplate == null) {
            return 0L;
        }
        
        String lockoutKey = LOCKOUT_PREFIX + email;
        return redisTemplate.getExpire(lockoutKey, TimeUnit.SECONDS);
    }
    
    /**
     * Get number of failed attempts for an email
     */
    public int getFailedAttemptCount(String email) {
        if (!rateLimitEnabled || redisTemplate == null) {
            return 0;
        }
        
        String attemptKey = ATTEMPT_PREFIX + email;
        String value = redisTemplate.opsForValue().get(attemptKey);
        return value != null ? Integer.parseInt(value) : 0;
    }
    
    /**
     * Get remaining attempts before lockout
     */
    public int getRemainingAttempts(String email) {
        int failedAttempts = getFailedAttemptCount(email);
        return Math.max(0, maxAttempts - failedAttempts);
    }
    
    /**
     * Record a failed login attempt
     */
    @Transactional
    public void recordFailedAttempt(String email, String ipAddress, String userAgent, String failureReason) {
        if (!rateLimitEnabled || redisTemplate == null) {
            logger.debug("Rate limiting disabled, skipping failed attempt recording");
            return;
        }
        
        // Save to database for audit trail
        LoginAttempt attempt = LoginAttempt.builder()
            .email(email)
            .ipAddress(ipAddress)
            .successful(false)
            .failureReason(failureReason)
            .userAgent(userAgent)
            .build();
        loginAttemptRepository.save(attempt);
        
        // Increment attempt counter in Redis
        String attemptKey = ATTEMPT_PREFIX + email;
        Long attempts = redisTemplate.opsForValue().increment(attemptKey);
        
        if (attempts == null) {
            attempts = 1L;
        }
        
        // Set expiration on first attempt
        if (attempts == 1) {
            redisTemplate.expire(attemptKey, attemptWindowMinutes, TimeUnit.MINUTES);
        }
        
        logger.debug("Failed login attempt {} of {} for email: {}", attempts, maxAttempts, email);
        
        // Lock account if max attempts reached
        if (attempts >= maxAttempts) {
            lockAccount(email);
            logger.warn("Account locked due to {} failed attempts: {}", attempts, email);
        }
    }
    
    /**
     * Record a successful login attempt
     */
    @Transactional
    public void recordSuccessfulAttempt(String email, String ipAddress, String userAgent) {
        // Save to database for audit trail
        LoginAttempt attempt = LoginAttempt.builder()
            .email(email)
            .ipAddress(ipAddress)
            .successful(true)
            .userAgent(userAgent)
            .build();
        loginAttemptRepository.save(attempt);
        
        // Clear failed attempts counter
        if (redisTemplate != null) {
            clearFailedAttempts(email);
        }
        
        logger.debug("Successful login recorded for email: {}", email);
    }
    
    /**
     * Lock an account temporarily
     */
    private void lockAccount(String email) {
        String lockoutKey = LOCKOUT_PREFIX + email;
        redisTemplate.opsForValue().set(lockoutKey, String.valueOf(System.currentTimeMillis()));
        redisTemplate.expire(lockoutKey, lockoutDurationMinutes, TimeUnit.MINUTES);
    }
    
    /**
     * Manually unlock an account (admin function)
     */
    public void unlockAccount(String email) {
        if (redisTemplate == null) {
            logger.warn("Redis not available, cannot unlock account: {}", email);
            return;
        }
        String lockoutKey = LOCKOUT_PREFIX + email;
        redisTemplate.delete(lockoutKey);
        clearFailedAttempts(email);
        logger.info("Account manually unlocked: {}", email);
    }
    
    /**
     * Clear failed attempts counter
     */
    private void clearFailedAttempts(String email) {
        if (redisTemplate == null) {
            return;
        }
        String attemptKey = ATTEMPT_PREFIX + email;
        redisTemplate.delete(attemptKey);
    }
    
    /**
     * Check if rate limit is enabled
     */
    public boolean isRateLimitEnabled() {
        return rateLimitEnabled;
    }
    
    /**
     * Get max attempts configuration
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }
    
    /**
     * Get lockout duration in minutes
     */
    public int getLockoutDurationMinutes() {
        return lockoutDurationMinutes;
    }
}

