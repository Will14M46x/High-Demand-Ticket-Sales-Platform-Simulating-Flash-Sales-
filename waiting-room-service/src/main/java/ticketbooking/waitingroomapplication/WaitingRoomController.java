package ticketbooking.waitingroomapplication;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/waiting-room")
public class WaitingRoomController {
    
    private static final Logger logger = LoggerFactory.getLogger(WaitingRoomController.class);
    private static final String QUEUE_KEY = "waitingRoomQueue";

    @Autowired
    private StringRedisTemplate redisTemplate;
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "waiting-room-service");
        return response;
    }

    /**
     * Join the waiting room queue (requires authentication)
     * Uses authenticated user ID from JWT token
     */
    @PostMapping("/join")
    public String joinQueue(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String email = (String) request.getAttribute("email");
        logger.info("User {} (ID: {}) joining waiting room", email, userId);
        
        String userIdStr = String.valueOf(userId);
        ListOperations<String, String> listOps = redisTemplate.opsForList();

        List<String> queue = listOps.range(QUEUE_KEY, 0, -1);
        if (queue != null && queue.contains(userIdStr)) {
            int position = queue.indexOf(userIdStr) + 1;
            logger.debug("User {} already in queue at position: {}", userId, position);
            return "You are already in the queue. Position: " + position;
        }

        listOps.rightPush(QUEUE_KEY, userIdStr);
        Long position = listOps.size(QUEUE_KEY);
        logger.info("User {} added to queue at position: {}", userId, position);
        return "You've been added to the waiting room. Position: " + position;
    }

    /**
     * Get authenticated user's status in the queue
     */
    @GetMapping("/status")
    public String getStatus(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String email = (String) request.getAttribute("email");
        logger.debug("User {} (ID: {}) checking queue status", email, userId);
        
        String userIdStr = String.valueOf(userId);
        ListOperations<String, String> listOps = redisTemplate.opsForList();
        List<String> queue = listOps.range(QUEUE_KEY, 0, -1);

        if (queue == null || !queue.contains(userIdStr)) {
            return "You're not currently in the queue.";
        }

        int position = queue.indexOf(userIdStr) + 1;
        return "Your current position: " + position;
    }

    /**
     * Release users from queue (admin operation - requires authentication)
     * In production, this should have role-based access control (ADMIN only)
     */
    @PostMapping("/release")
    public String releaseUsers(@RequestParam(defaultValue = "50") int count, HttpServletRequest request) {
        Long adminUserId = (Long) request.getAttribute("userId");
        String adminEmail = (String) request.getAttribute("email");
        logger.info("Admin user {} (ID: {}) releasing {} users from queue", adminEmail, adminUserId, count);
        
        ListOperations<String, String> listOps = redisTemplate.opsForList();
        List<String> releasedUsers = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String user = listOps.leftPop(QUEUE_KEY);
            if (user == null) break;
            releasedUsers.add(user);
        }

        if (releasedUsers.isEmpty()) {
            logger.debug("No users in queue to release");
            return "No users currently waiting to be released.";
        }

        logger.info("Released {} users from queue: {}", releasedUsers.size(), releasedUsers);
        return "Released users: " + releasedUsers;
    }

    @GetMapping("/stats")
    public Map<String, Object> getQueueStats() {
        ListOperations<String, String> listOps = redisTemplate.opsForList();
        Long queueSize = listOps.size(QUEUE_KEY);

        Map<String, Object> stats = new HashMap<>();
        stats.put("queueSize", queueSize == null ? 0 : queueSize);
        stats.put("timestamp", new Date().toString());
        stats.put("queuePreview", listOps.range(QUEUE_KEY, 0, Math.min(5, (queueSize == null ? 0 : queueSize) - 1)));

        return stats;
    }

    /**
     * Clear entire queue (admin operation - requires authentication)
     * In production, this should have role-based access control (ADMIN only)
     */
    @DeleteMapping("/clear")
    public String clearQueue(HttpServletRequest request) {
        Long adminUserId = (Long) request.getAttribute("userId");
        String adminEmail = (String) request.getAttribute("email");
        logger.warn("Admin user {} (ID: {}) clearing entire waiting room queue", adminEmail, adminUserId);
        
        Long queueSize = redisTemplate.opsForList().size(QUEUE_KEY);
        redisTemplate.delete(QUEUE_KEY);
        
        logger.info("Waiting room queue cleared. {} users were removed", queueSize);
        return "Waiting room queue has been cleared. " + queueSize + " users were removed.";
    }
}
