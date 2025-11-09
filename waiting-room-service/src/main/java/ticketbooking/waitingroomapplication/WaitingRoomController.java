package ticketbooking.waitingroomapplication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/waiting-room")
public class WaitingRoomController {

    private static final String QUEUE_KEY = "waitingRoomQueue";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @PostMapping("/join")
    public String joinQueue(@RequestParam String userId) {
        ListOperations<String, String> listOps = redisTemplate.opsForList();

        List<String> queue = listOps.range(QUEUE_KEY, 0, -1);
        if (queue != null && queue.contains(userId)) {
            int position = queue.indexOf(userId) + 1;
            return "You are already in the queue. Position: " + position;
        }

        listOps.rightPush(QUEUE_KEY, userId);
        Long position = listOps.size(QUEUE_KEY);
        return "You’ve been added to the waiting room. Position: " + position;
    }

    @GetMapping("/status")
    public String getStatus(@RequestParam String userId) {
        ListOperations<String, String> listOps = redisTemplate.opsForList();
        List<String> queue = listOps.range(QUEUE_KEY, 0, -1);

        if (queue == null || !queue.contains(userId)) {
            return "You’re not currently in the queue.";
        }

        int position = queue.indexOf(userId) + 1;
        return "Your current position: " + position;
    }

    @PostMapping("/release")
    public String releaseUsers(@RequestParam(defaultValue = "50") int count) {
        ListOperations<String, String> listOps = redisTemplate.opsForList();
        List<String> releasedUsers = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String user = listOps.leftPop(QUEUE_KEY);
            if (user == null) break;
            releasedUsers.add(user);
        }

        if (releasedUsers.isEmpty()) {
            return "No users currently waiting to be released.";
        }

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

    @DeleteMapping("/clear")
    public String clearQueue() {
        redisTemplate.delete(QUEUE_KEY);
        return "Waiting room queue has been cleared.";
    }
}
