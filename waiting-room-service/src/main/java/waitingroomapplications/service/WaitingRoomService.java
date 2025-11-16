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
    @org.springframework.beans.factory.annotation.Value("${waiting-room.estimated-wait-per-user:30}")
    private long estimatedWaitPerUserSeconds;

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

            // Execute remove/add atomically in a Redis transaction
            redisTemplate.execute((operations) -> {
                operations.multi();
                for (String userId : admittedUsers) {
                    operations.opsForZSet().remove(queueKey, userId);
                    operations.opsForSet().add(admittedKey, userId);
                }
                operations.exec();
                return null;
            });

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

            long waiting = Optional.ofNullable(redisTemplate.opsForZSet().size(queueKey)).orElse(0L);
            long admitted = Optional.ofNullable(redisTemplate.opsForSet().size(admittedKey)).orElse(0L);

            String estimatedWait = formatEstimatedWait(waiting * estimatedWaitPerUserSeconds);

            QueueStatusResponse response = QueueStatusResponse.builder()
                    .totalWaiting(waiting)
                    .totalAdmitted(admitted)
                    .estimatedWaitTime(estimatedWait)
                    .build();

            log.info("Queue status for event {}: {} waiting, {} admitted, estimated wait {}",
                    eventId, waiting, admitted, estimatedWait);

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
            // Port and redis host:port should be populated by controller using config, not hardcoded here
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

    private String formatEstimatedWait(long seconds) {
        if (seconds < 60) {
            return seconds + " seconds";
        }
        long mins = seconds / 60;
        long secs = seconds % 60;
        if (secs == 0) {
            return mins + " minutes";
        }
        return mins + "m " + secs + "s";
    }

    public String estimateWaitForPositions(long positions) {
        long seconds = positions * estimatedWaitPerUserSeconds;
        return formatEstimatedWait(seconds);
    }
}
