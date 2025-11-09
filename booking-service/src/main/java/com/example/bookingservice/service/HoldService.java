package com.example.bookingservice.service;

import com.example.bookingservice.model.TicketHold;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class HoldService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String HOLD_KEY_PREFIX = "ticket:hold:";
    private static final long HOLD_DURATION_MINUTES = 10; // 10-minute hold

    public HoldService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Create a temporary hold for tickets
     */
    public TicketHold createHold(Long eventId, Long userId, int quantity, double totalPrice) {
        String holdId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(HOLD_DURATION_MINUTES);

        TicketHold hold = TicketHold.builder()
                .holdId(holdId)
                .eventId(eventId)
                .userId(userId)
                .quantity(quantity)
                .totalPrice(totalPrice)
                .createdAt(now)
                .expiresAt(expiresAt)
                .isActive(true)
                .build();

        // Store in Redis with TTL
        String key = HOLD_KEY_PREFIX + holdId;
        redisTemplate.opsForValue().set(key, hold, HOLD_DURATION_MINUTES, TimeUnit.MINUTES);

        return hold;
    }

    /**
     * Get a hold by ID
     */
    public TicketHold getHold(String holdId) {
        String key = HOLD_KEY_PREFIX + holdId;
        Object value = redisTemplate.opsForValue().get(key);
        
        if (value instanceof TicketHold) {
            TicketHold hold = (TicketHold) value;
            
            // Check if hold is still valid
            if (hold.getExpiresAt().isAfter(LocalDateTime.now())) {
                return hold;
            } else {
                // Hold expired, mark as inactive
                hold.setActive(false);
                return hold;
            }
        }
        
        return null;
    }

    /**
     * Release a hold (delete from Redis)
     */
    public boolean releaseHold(String holdId) {
        String key = HOLD_KEY_PREFIX + holdId;
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    /**
     * Check if a hold exists and is valid
     */
    public boolean isHoldValid(String holdId) {
        TicketHold hold = getHold(holdId);
        return hold != null && hold.isActive();
    }

    /**
     * Extend hold duration (if needed)
     */
    public boolean extendHold(String holdId, long additionalMinutes) {
        TicketHold hold = getHold(holdId);
        if (hold != null && hold.isActive()) {
            hold.setExpiresAt(hold.getExpiresAt().plusMinutes(additionalMinutes));
            String key = HOLD_KEY_PREFIX + holdId;
            redisTemplate.opsForValue().set(key, hold, 
                HOLD_DURATION_MINUTES + additionalMinutes, TimeUnit.MINUTES);
            return true;
        }
        return false;
    }
}
