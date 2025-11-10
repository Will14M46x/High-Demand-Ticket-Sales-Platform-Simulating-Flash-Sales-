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
        System.out.println("🔍 DEBUG: Looking for hold with key: " + key);

        Object value = redisTemplate.opsForValue().get(key);
        System.out.println("🔍 DEBUG: Retrieved value from Redis: " + value);
        System.out.println("🔍 DEBUG: Value type: " + (value != null ? value.getClass().getName() : "null"));

        if (value == null) {
            System.out.println("❌ DEBUG: No value found in Redis!");
            return null;
        }

        TicketHold hold = null;

        // Handle both TicketHold object and LinkedHashMap (from Redis deserialization)
        if (value instanceof TicketHold) {
            hold = (TicketHold) value;
            System.out.println("✅ DEBUG: Value is already a TicketHold object");
        } else if (value instanceof java.util.Map) {
            // Redis returned a Map - need to convert it to TicketHold
            System.out.println("🔄 DEBUG: Converting Map to TicketHold");
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) value;

            hold = TicketHold.builder()
                    .holdId((String) map.get("holdId"))
                    .eventId(((Number) map.get("eventId")).longValue())
                    .userId(((Number) map.get("userId")).longValue())
                    .quantity(((Number) map.get("quantity")).intValue())
                    .totalPrice(((Number) map.get("totalPrice")).doubleValue())
                    .createdAt(LocalDateTime.parse((String) map.get("createdAt")))
                    .expiresAt(LocalDateTime.parse((String) map.get("expiresAt")))
                    .isActive((Boolean) map.get("active"))
                    .build();
            System.out.println("✅ DEBUG: Successfully converted to TicketHold");
        }

        if (hold != null) {
            System.out.println("🔍 DEBUG: Hold found! ExpiresAt: " + hold.getExpiresAt());
            System.out.println("🔍 DEBUG: Current time: " + LocalDateTime.now());
            System.out.println("🔍 DEBUG: Is active: " + hold.isActive());

            // Check if hold is still valid
            if (hold.getExpiresAt().isAfter(LocalDateTime.now())) {
                System.out.println("✅ DEBUG: Hold is valid!");
                return hold;
            } else {
                // Hold expired, mark as inactive
                System.out.println("❌ DEBUG: Hold has expired!");
                hold.setActive(false);
                return hold;
            }
        }

        System.out.println("❌ DEBUG: Could not process hold!");
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