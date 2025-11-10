package waitingroomapplications.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import waitingroomapplications.dto.QueueStatusResponse;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaitingRoomService {

    private static final String QUEUE_KEY_PREFIX = "waitingroom:queue:";
    private static final String ADMITTED_KEY_PREFIX = "waitingroom:admitted:";

    private final RedisTemplate<String, String> redisTemplate;

    public Integer joinQueue(String userId, Long eventId, Integer quantity) {
        try {
            String queueKey = QUEUE_KEY_PREFIX + eventId;
            double score = System.currentTimeMillis();

            redisTemplate.opsForZSet().add(queueKey, userId, score);

            Long rank = redisTemplate.opsForZSet().rank(queueKey, userId);

            log.info("User {} joined queue {} at position {}", userId, eventId, rank);

            return rank != null ? rank.intValue() + 1 : 1;

        } catch (Exception e) {
            log.error("Error joining queue for user {}: ", userId, e);
            throw new RuntimeException("Failed to join queue", e);
        }
    }


    public Integer getPosition(String userId, Long eventId) {
        try {
            String queueKey = QUEUE_KEY_PREFIX + eventId;

            Long rank = redisTemplate.opsForZSet().rank(queueKey, userId);

            if (rank == null) {
                String admittedKey = ADMITTED_KEY_PREFIX + eventId;
                Boolean isAdmitted = redisTemplate.opsForSet().isMember(admittedKey, userId);

                if (Boolean.TRUE.equals(isAdmitted)) {
                    log.info("User {} already admitted to event {}", userId, eventId);
                    return 0;
                }

                log.warn("User {} not found in queue for event {}", userId, eventId);
                return null;
            }

            return rank.intValue() + 1;

        } catch (Exception e) {
            log.error("Error getting position for user {}: ", userId, e);
            throw new RuntimeException("Failed to get position", e);
        }
    }

    public List<String> admitBatch(Integer batchSize, Long eventId) {
        try {
            String queueKey = QUEUE_KEY_PREFIX + eventId;
            String admittedKey = ADMITTED_KEY_PREFIX + eventId;

            Set<String> usersToAdmit = redisTemplate.opsForZSet().range(queueKey, 0, batchSize - 1);

            if (usersToAdmit == null || usersToAdmit.isEmpty()) {
                log.info("No users in queue to admit for event {}", eventId);
                return Collections.emptyList();
            }

            List<String> admittedUsers = new ArrayList<>(usersToAdmit);

            for (String userId : admittedUsers) {
                redisTemplate.opsForZSet().remove(queueKey, userId);
                redisTemplate.opsForSet().add(admittedKey, userId);
            }

            log.info("Admitted {} users from queue for event {}", admittedUsers.size(), eventId);

            return admittedUsers;

        } catch (Exception e) {
            log.error("Error admitting batch for event {}: ", eventId, e);
            throw new RuntimeException("Failed to admit batch", e);
        }
    }

    public QueueStatusResponse getQueueStatus(Long eventId) {
        try {
            String queueKey = QUEUE_KEY_PREFIX + eventId;
            String admittedKey = ADMITTED_KEY_PREFIX + eventId;

            Long queueSize = redisTemplate.opsForZSet().size(queueKey);

            Long admittedCount = redisTemplate.opsForSet().size(admittedKey);

            QueueStatusResponse response = QueueStatusResponse.builder()
                    .totalWaiting(queueSize != null ? queueSize : 0L)
                    .totalAdmitted(admittedCount != null ? admittedCount : 0L)
                    .queueLength(queueSize != null ? queueSize : 0L)
                    .build();

            log.info("Queue status for event {}: {} waiting, {} admitted",
                    eventId, response.getTotalWaiting(), response.getTotalAdmitted());

            return response;

        } catch (Exception e) {
            log.error("Error getting queue status for event {}: ", eventId, e);
            throw new RuntimeException("Failed to get queue status", e);
        }
    }


    public boolean removeUser(String userId, Long eventId) {
        try {
            String queueKey = QUEUE_KEY_PREFIX + eventId;


            Long removed = redisTemplate.opsForZSet().remove(queueKey, userId);

            boolean wasRemoved = removed != null && removed > 0;

            if (wasRemoved) {
                log.info("User {} removed from queue for event {}", userId, eventId);
            } else {
                log.warn("User {} not found in queue for event {}", userId, eventId);
            }

            return wasRemoved;

        } catch (Exception e) {
            log.error("Error removing user {} from queue: ", userId, e);
            throw new RuntimeException("Failed to remove user", e);
        }
    }


    public Map<String, Object> checkHealth() {
        Map<String, Object> health = new HashMap<>();

        try {
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();

            boolean isConnected = "PONG".equalsIgnoreCase(pong);

            health.put("status", isConnected ? "UP" : "DOWN");
            health.put("redisConnected", isConnected);
            health.put("service", "waiting-room-service");
            health.put("port", 8085);
            health.put("redis", "localhost:6379");
            health.put("timestamp", System.currentTimeMillis());

            log.info("Health check: Redis connection {}", isConnected ? "UP" : "DOWN");

        } catch (Exception e) {
            log.error("Health check failed: ", e);
            health.put("status", "DOWN");
            health.put("redisConnected", false);
            health.put("error", e.getMessage());
        }

        return health;
    }
}