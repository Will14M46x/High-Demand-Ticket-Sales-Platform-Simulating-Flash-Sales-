package ticketbooking.waitingroomapplication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/waiting-room")
public class WaitingRoomController {

    private final String QUEUE_KEY = "waitingRoomQueue";
    private static final int MAX_QUEUE_SIZE = 500;

    @Autowired
    private StringRedisTemplate redisTemplate;

    // Join queue
    @PostMapping("/join")
    public String joinQueue(@RequestParam String userId) {
        ListOperations<String, String> listOps = redisTemplate.opsForList();
        Long queueSize = listOps.size(QUEUE_KEY);

        if (queueSize != null && queueSize >= MAX_QUEUE_SIZE) {
            return "Queue is full. Please try again later.";
        }

        // Avoid duplicate entries
        if (listOps.range(QUEUE_KEY, 0, -1).contains(userId)) {
            return "You are already in the queue. Position: " + (listOps.range(QUEUE_KEY, 0, -1).indexOf(userId) + 1);
        }

        listOps.rightPush(QUEUE_KEY, userId);
        return "You’ve been added to the waiting room. Position: " + listOps.size(QUEUE_KEY);
    }

    // Check position
    @GetMapping("/status")
    public String getStatus(@RequestParam String userId) {
        ListOperations<String, String> listOps = redisTemplate.opsForList();
        List<String> queue = listOps.range(QUEUE_KEY, 0, -1);
        if (queue == null || !queue.contains(userId)) {
            return "You’re not in the queue.";
        }
        int position = queue.indexOf(userId) + 1;
        return "Your current position: " + position;
    }

    // Release users in batches
    @PostMapping("/release")
    public String releaseUsers(@RequestParam int count) {
        ListOperations<String, String> listOps = redisTemplate.opsForList();
        List<String> releasedUsers = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String user = listOps.leftPop(QUEUE_KEY);
            if (user == null) break;
            releasedUsers.add(user);
        }

        return "Released users: " + releasedUsers;
    }

    // Current queue size
    @GetMapping("/queue-size")
    public String getQueueSize() {
        Long size = redisTemplate.opsForList().size(QUEUE_KEY);
        return "Current queue size: " + (size == null ? 0 : size);
    }
}
