package waitingroomapplications;

import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import waitingroomapplications.dto.AdmitBatchRequest;
import waitingroomapplications.dto.JoinQueueRequest;
import waitingroomapplications.dto.QueuePositionResponse;
import waitingroomapplications.dto.QueueStatusResponse;
import waitingroomapplications.service.WaitingRoomService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/waitingroom")
@RequiredArgsConstructor
@Validated
public class WaitingRoomController {

    private final WaitingRoomService waitingRoomService;

    @org.springframework.beans.factory.annotation.Value("${server.port}")
    private int serverPort;

    @org.springframework.beans.factory.annotation.Value("${spring.data.redis.host}")
    private String redisHost;

    @org.springframework.beans.factory.annotation.Value("${spring.data.redis.port}")
    private int redisPort;

    @PostMapping("/join")
    public ResponseEntity<?> joinQueue(@Valid @RequestBody JoinQueueRequest request) {
        try {
            log.info("User {} joining queue for event {}", request.getUserId(), request.getEventId());

            Integer position = waitingRoomService.joinQueue(
                    request.getUserId(),
                    request.getEventId(),
                    request.getRequestedQuantity()
            );

            QueuePositionResponse response = QueuePositionResponse.builder()
                    .userId(request.getUserId())
                    .position(position)
                    .estimatedWaitTime(waitingRoomService.estimateWaitForPositions(position))
                    .build();

            log.info("User {} added to queue at position {}", request.getUserId(), position);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error joining queue: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error joining queue: " + e.getMessage()));
        }
    }

    @GetMapping("/position/{userId}")
    public ResponseEntity<?> getPosition(@PathVariable String userId, @RequestParam Long eventId) {
        try {
            log.info("Getting position for user {} in event {}", userId, eventId);

            waitingroomapplications.dto.PositionInfo info = waitingRoomService.getPositionInfo(userId, eventId);

            if (info.getStatus() == waitingroomapplications.dto.PositionStatus.NOT_FOUND) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found in queue"));
            }

            String waitText = (info.getStatus() == waitingroomapplications.dto.PositionStatus.ADMITTED)
                    ? "Admitted - ready to proceed"
                    : waitingRoomService.estimateWaitForPositions(info.getPosition() == null ? 0 : info.getPosition());

            QueuePositionResponse response = QueuePositionResponse.builder()
                    .userId(userId)
                    .position(info.getPosition())
                    .estimatedWaitTime(waitText)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting position: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error getting position: " + e.getMessage()));
        }
    }

    @PostMapping("/admit")
    public ResponseEntity<?> admitBatch(@Valid @RequestBody AdmitBatchRequest request) {
        try {
            log.info("Admitting batch of {} users for event {}", request.getBatchSize(), request.getEventId());

            List<String> admittedUsers = waitingRoomService.admitBatch(request.getBatchSize(), request.getEventId());

            Map<String, Object> response = Map.of(
                    "admittedUsers", admittedUsers,
                    "count", admittedUsers.size()
            );

            log.info("Admitted {} users for event {}", admittedUsers.size(), request.getEventId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error admitting batch: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error admitting batch: " + e.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@RequestParam Long eventId) {
        try {
            log.info("Getting queue status for event {}", eventId);

            QueueStatusResponse response = waitingRoomService.getQueueStatus(eventId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting queue status: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error getting queue status: " + e.getMessage()));
        }
    }

    @DeleteMapping("/remove/{userId}")
    public ResponseEntity<?> removeUser(@PathVariable String userId, @RequestParam Long eventId) {
        try {
            log.info("Removing user {} from event {}", userId, eventId);

            boolean removed = waitingRoomService.removeUser(userId, eventId);

            if (removed) {
                return ResponseEntity.ok(Map.of(
                        "message", "User successfully removed from queue",
                        "userId", userId
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found in queue"));
            }

        } catch (Exception e) {
            log.error("Error removing user: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error removing user: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        try {
            log.info("Health check requested");

            Map<String, Object> health = waitingRoomService.checkHealth();

            // Enrich with runtime config details
            health.put("port", serverPort);
            health.put("redis", redisHost + ":" + redisPort);

            if (Boolean.TRUE.equals(health.get("redisConnected"))) {
                return ResponseEntity.ok(health);
            } else {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
            }

        } catch (Exception e) {
            log.error("Health check failed: ", e);
            Map<String, Object> errorHealth = Map.of(
                    "status", "DOWN",
                    "redisConnected", false,
                    "error", e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorHealth);
        }
    }
}
