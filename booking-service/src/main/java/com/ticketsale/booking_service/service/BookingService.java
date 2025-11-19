package com.ticketsale.booking_service.service;

import com.ticketsale.booking_service.dto.BookingRequest;
import com.ticketsale.booking_service.dto.OrderResponse;
import com.ticketsale.booking_service.dto.QueuePositionResponse;
import com.ticketsale.booking_service.model.Order;
import com.ticketsale.booking_service.model.OrderStatus;
import com.ticketsale.booking_service.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled; // ADDED: For cleanup job
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    // --- DEPENDENCIES ---
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private StringRedisTemplate redisTemplate;

    // --- CONFIGURATION ---
    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    @Value("${waitingroom.service.url}")
    private String waitingRoomServiceUrl;

    // Changed to 30 seconds for easier testing of the expired hold logic
    private static final Duration HOLD_DURATION = Duration.ofSeconds(30);
    // private static final Duration HOLD_DURATION = Duration.ofMinutes(5); // Original value

    // --- HELPER METHOD: Admission Check ---
    /**
     * Checks the Waiting Room Service to see if a user has been "admitted" (position == 0).
     */
    private boolean isUserAdmitted(String userId, Long eventId) {
        // We use the public /position endpoint for service-to-service communication
        String checkPositionUrl = String.format(
                "%s/position/%s?eventId=%d",
                waitingRoomServiceUrl,
                userId,
                eventId
        );

        try {
            // Note: This RestTemplate call is unsecured because we made the /position endpoint public
            ResponseEntity<QueuePositionResponse> response = restTemplate.getForEntity(
                    checkPositionUrl,
                    QueuePositionResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Integer position = response.getBody().getPosition();
                // Position 0 means "admitted"
                return position != null && position == 0;
            }
            return false;
        } catch (HttpClientErrorException.NotFound e) {
            // 404 means the user is not in the queue/admitted set
            return false;
        } catch (Exception e) {
            System.err.println("Error checking waiting room status: " + e.getMessage());
            return false;
        }
    }


    // --- CORE METHOD: Create Booking ---
    /**
     * Creates a new order. Requires user to be admitted by the waiting room.
     */
    @Transactional
    public OrderResponse createBooking(BookingRequest request, Long userId) {

        // --- STEP 1: CHECK WAITING ROOM ---
        // We use the secure userId extracted from the token
        String userIdStr = String.valueOf(userId);

        if (!isUserAdmitted(userIdStr, request.getEventId())) {
            throw new SecurityException("User has not been admitted from the waiting room.");
        }

        // --- STEP 2: CALL INVENTORY SERVICE TO RESERVE ---
        String reserveUrl = inventoryServiceUrl + "/" + request.getEventId() + "/reserve?quantity=" + request.getQuantity();

        try {
            // This service-to-service call is permitted (permitAll) in InventorySecurityConfig
            ResponseEntity<String> response = restTemplate.postForEntity(reserveUrl, null, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Inventory service rejected reservation: " + response.getBody());
            }
        } catch (HttpClientErrorException e) {
            // Catches 409 Conflict if insufficient stock/locking fails
            throw new RuntimeException("Failed to reserve tickets (e.g., insufficient stock or concurrency conflict).");
        } catch (Exception e) {
            throw new RuntimeException("Error communicating with inventory service.");
        }

        // --- STEP 3: CREATE PENDING ORDER ---
        Order order = new Order();
        order.setUserId(userId); // Using the secure ID
        order.setEventId(request.getEventId());
        order.setQuantity(request.getQuantity());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        // Mock total amount
        order.setTotalAmount(new BigDecimal("50.00").multiply(new BigDecimal(request.getQuantity())));

        Order savedOrder = orderRepository.save(order);

        // --- STEP 4: PLACE TEMPORARY HOLD (Redis) ---
        String holdKey = "ticket_hold:" + savedOrder.getId();
        String holdValue = savedOrder.getEventId() + ":" + savedOrder.getQuantity();
        redisTemplate.opsForValue().set(holdKey, holdValue, HOLD_DURATION);

        return mapToOrderResponse(savedOrder, "Booking pending. Please complete payment within 5 minutes.");
    }

    // --- CORE METHOD: Confirm Payment ---
    /**
     * Finalizes the order (simulating payment success) OR handles an expired hold.
     */
    @Transactional
    public OrderResponse confirmPayment(Long orderId) {
        // 1. Find the order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            return mapToOrderResponse(order, "Order is already finalized. Current state: " + order.getStatus());
        }

        // 2. Check if the hold in Redis still exists
        String holdKey = "ticket_hold:" + order.getId();
        Boolean holdExists = redisTemplate.hasKey(holdKey);

        if (holdExists == null || !holdExists) {
            // Hold has EXPIRED! This is the compensating transaction logic.
            order.setStatus(OrderStatus.EXPIRED);
            Order expiredOrder = orderRepository.save(order); // Commits EXPIRED status
            releaseTicketsInInventory(order.getEventId(), order.getQuantity()); // Releases tickets
            return mapToOrderResponse(expiredOrder, "Hold expired. Payment failed.");
        }

        // 3. Hold exists! Payment is successful.
        redisTemplate.delete(holdKey);
        order.setStatus(OrderStatus.PAID);
        Order confirmedOrder = orderRepository.save(order); // Commits PAID status

        return mapToOrderResponse(confirmedOrder, "Payment successful. Order confirmed.");
    }

    // --- BACKGROUND METHOD: Scheduled Cleanup ---
    /**
     * Background Task: Runs every minute to clean up orders stuck in PENDING status
     * after their hold duration has passed.
     */
    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    @Transactional
    public void cleanupExpiredOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minus(HOLD_DURATION);

        // Find orders that are PENDING and were created BEFORE the cutoff time
        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING, cutoff);

        for (Order order : expiredOrders) {
            try {
                // Mark as expired
                order.setStatus(OrderStatus.EXPIRED);
                orderRepository.save(order);

                // Release tickets back to inventory (Compensating Transaction)
                releaseTicketsInInventory(order.getEventId(), order.getQuantity());

                // Remove the redis key just in case
                String holdKey = "ticket_hold:" + order.getId();
                redisTemplate.delete(holdKey);

                System.out.println("Automatically expired order ID: " + order.getId());
            } catch (Exception e) {
                System.err.println("Failed to cleanup order " + order.getId() + ": " + e.getMessage());
            }
        }
    }

    // --- HELPER METHOD: Release Tickets ---
    /**
     * Calls the Inventory Service to release tickets back into the pool.
     */
    private void releaseTicketsInInventory(Long eventId, int quantity) {
        String releaseUrl = inventoryServiceUrl + "/" + eventId + "/release?quantity=" + quantity;

        try {
            // POST to inventory service to increment the ticket count
            restTemplate.postForEntity(releaseUrl, null, String.class);
            System.out.println("Successfully notified inventory service to release " + quantity + " tickets for event " + eventId);
        } catch (Exception e) {
            // CRITICAL: A real system would log this error and add it to a retry queue.
            System.err.println("CRITICAL ERROR: Failed to release tickets in inventory. " + e.getMessage());
        }
    }

    // --- HELPER METHOD: DTO Mapping ---
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