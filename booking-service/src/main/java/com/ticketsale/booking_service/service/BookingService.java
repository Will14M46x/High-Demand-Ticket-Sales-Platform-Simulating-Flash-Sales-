package com.ticketsale.booking_service.service;

import com.ticketsale.booking_service.dto.BookingRequest;
import com.ticketsale.booking_service.dto.OrderResponse;
// --- IMPORT THE NEW DTO ---
import com.ticketsale.booking_service.dto.QueuePositionResponse;
import com.ticketsale.booking_service.model.Order;
import com.ticketsale.booking_service.model.OrderStatus;
import com.ticketsale.booking_service.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class BookingService {

    // --- (Keep all your existing @Autowired fields) ---
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private StringRedisTemplate redisTemplate;

    // --- (Keep your existing @Value) ---
    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    // --- ADD THE NEW @Value ---
    @Value("${waitingroom.service.url}")
    private String waitingRoomServiceUrl;

    private static final Duration HOLD_DURATION = Duration.ofMinutes(5);


    // --- THIS IS THE NEW HELPER METHOD ---
    /**
     * Checks the Waiting Room Service to see if a user has been "admitted".
     * @return true if position is 0 (admitted), false otherwise.
     */
    private boolean isUserAdmitted(String userId, Long eventId) {
        // Build the URL: http://localhost:8087/api/waitingroom/position/user_123?eventId=1
        String checkPositionUrl = String.format(
                "%s/position/%s?eventId=%d",
                waitingRoomServiceUrl,
                userId,
                eventId
        );

        try {
            // Make the GET request
            ResponseEntity<QueuePositionResponse> response = restTemplate.getForEntity(
                    checkPositionUrl,
                    QueuePositionResponse.class
            );

            // Check the response body
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Integer position = response.getBody().getPosition();
                // Position 0 means "admitted"
                return position != null && position == 0;
            }

            // User not found or other issue
            return false;

        } catch (HttpClientErrorException.NotFound e) {
            // 404 Not Found means the user isn't even in the queue
            System.err.println("User not found in queue: " + userId);
            return false;
        } catch (Exception e) {
            // Any other error (e.g., service down), we fail safely
            System.err.println("Error checking waiting room status: " + e.getMessage());
            return false;
        }
    }

    // --- THIS IS THE MODIFIED createBooking METHOD ---
    @Transactional
    public OrderResponse createBooking(BookingRequest request) {

        // --- STEP 1: CHECK WAITING ROOM ---
        // We convert the Long userId to a String for the waiting room API
        String userIdStr = String.valueOf(request.getUserId());

        if (!isUserAdmitted(userIdStr, request.getEventId())) {
            System.err.println("User " + userIdStr + " REJECTED. Not admitted from waiting room.");
            // We'll handle this in the controller
            throw new SecurityException("User has not been admitted from the waiting room.");
        }

        System.out.println("User " + userIdStr + " VERIFIED. Admitted from waiting room.");

        // --- Step 2: Call Inventory Service (Original logic) ---
        String reserveUrl = inventoryServiceUrl + "/" + request.getEventId() + "/reserve?quantity=" + request.getQuantity();

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(reserveUrl, null, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Inventory service rejected reservation: " + response.getBody());
            }
            System.out.println("Inventory service confirmed reservation.");
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Failed to reserve tickets (e.g., insufficient stock or concurrency conflict).");
        } catch (Exception e) {
            throw new RuntimeException("Error communicating with inventory service.");
        }

        // --- Step 3: Create PENDING order (Original logic) ---
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setEventId(request.getEventId());
        order.setQuantity(request.getQuantity());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setTotalAmount(new BigDecimal("50.00").multiply(new BigDecimal(request.getQuantity())));
        Order savedOrder = orderRepository.save(order);

        // --- Step 4: Place temporary hold in Redis (Original logic) ---
        String holdKey = "ticket_hold:" + savedOrder.getId();
        String holdValue = savedOrder.getEventId() + ":" + savedOrder.getQuantity();
        redisTemplate.opsForValue().set(holdKey, holdValue, HOLD_DURATION);
        System.out.println("Placed hold in Redis for order: " + savedOrder.getId());

        // --- Step 5: Return response (Original logic) ---
        return mapToOrderResponse(savedOrder, "Booking pending. Please complete payment within 5 minutes.");
    }


    /**
     * Simulates a successful payment OR handles an expired hold.
     * Confirms the hold and updates the order status to PAID,
     * OR updates status to EXPIRED and releases tickets.
     */
    @Transactional
    public OrderResponse confirmPayment(Long orderId) {
        // 1. Find the order in our database
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        // 2. Check if the order is already in a final state
        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.EXPIRED) {
            // It's already done, just return its status
            return mapToOrderResponse(order, "Order is already finalized. Current state: " + order.getStatus());
        }

        // 3. Check if the order is PENDING (it should be)
        if (order.getStatus() != OrderStatus.PENDING) {
            // This case shouldn't happen, but good to check
            return mapToOrderResponse(order, "Order is not in PENDING state. Current state: " + order.getStatus());
        }

        // 4. Check if the hold in Redis still exists
        String holdKey = "ticket_hold:" + order.getId();
        Boolean holdExists = redisTemplate.hasKey(holdKey);

        if (holdExists == null || !holdExists) {
            // Hold has EXPIRED! The payment fails.
            System.err.println("Hold expired for order: " + orderId);

            // Set order status to EXPIRED
            order.setStatus(OrderStatus.EXPIRED);
            Order expiredOrder = orderRepository.save(order); // This will now commit

            // Tell the inventory-service to release the tickets
            releaseTicketsInInventory(order.getEventId(), order.getQuantity());

            // --- THIS IS THE FIX ---
            // Instead of throwing, we RETURN the failed DTO.
            // The transaction completes successfully, and the EXPIRED status is saved.
            return mapToOrderResponse(expiredOrder, "Hold expired. Payment failed.");
        }

        // 5. Hold exists! Payment is successful.
        // First, delete the Redis key to confirm the hold
        redisTemplate.delete(holdKey);
        System.out.println("Confirmed and deleted Redis hold for order: " + orderId);

        // 6. Update the order status to PAID
        order.setStatus(OrderStatus.PAID);
        Order confirmedOrder = orderRepository.save(order);

        return mapToOrderResponse(confirmedOrder, "Payment successful. Order confirmed.");
    }

    /**
     * Calls the Inventory Service to release tickets back into the pool.
     */
    private void releaseTicketsInInventory(Long eventId, int quantity) {
        String releaseUrl = inventoryServiceUrl + "/" + eventId + "/release?quantity=" + quantity;

        try {
            // Make the POST request:
            // http://localhost:8085/api/inventory/events/1/release?quantity=2
            restTemplate.postForEntity(releaseUrl, null, String.class);
            System.out.println("Successfully notified inventory service to release " + quantity + " tickets for event " + eventId);
        } catch (Exception e) {
            // This is a critical problem.
            // If this fails, the inventory count will be wrong.
            // A real-world system would add this to a retry queue.
            System.err.println("CRITICAL ERROR: Failed to release tickets in inventory. " + e.getMessage());
            // You would add compensating transaction logic here.
        }
    }

    // Helper to map Entity to DTO
    private OrderResponse mapToOrderResponse(Order order, String message) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .eventId(order.getEventId())
                .quantity(order.getQuantity())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .message(message)
                .build();
    }
}