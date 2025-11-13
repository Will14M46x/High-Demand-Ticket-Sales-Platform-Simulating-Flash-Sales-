package com.ticketsale.booking_service.service;

import com.ticketsale.booking_service.dto.BookingRequest;
import com.ticketsale.booking_service.dto.OrderResponse;
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

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate; // For temporary holds [cite: 16]

    // This injects the URL from our application.yml
    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    // Set hold time to 5 minutes, as per spec [cite: 16]
    private static final Duration HOLD_DURATION = Duration.ofMinutes(5);

    @Transactional
    public OrderResponse createBooking(BookingRequest request) {

        // --- Step 1: Call Inventory Service to reserve tickets ---
        // This is the Inter-Service Communication (REST) [cite: 8]
        String reserveUrl = inventoryServiceUrl + "/" + request.getEventId() + "/reserve?quantity=" + request.getQuantity();

        try {
            // Make the POST request:
            // http://localhost:8085/api/inventory/events/1/reserve?quantity=2
            ResponseEntity<String> response = restTemplate.postForEntity(reserveUrl, null, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Inventory service rejected reservation: " + response.getBody());
            }
            System.out.println("Inventory service confirmed reservation.");

        } catch (HttpClientErrorException e) {
            // This catches the 409 Conflict if inventory is full
            System.err.println("Failed to reserve tickets: " + e.getResponseBodyAsString());
            throw new RuntimeException("Failed to reserve tickets (e.g., insufficient stock or concurrency conflict).");
        } catch (Exception e) {
            // This catches network errors, e.g., inventory-service is down
            System.err.println("Error calling inventory service: " + e.getMessage());
            throw new RuntimeException("Error communicating with inventory service.");
        }

        // --- Step 2: Create PENDING order in our local DB ---
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setEventId(request.getEventId());
        order.setQuantity(request.getQuantity());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        // Mock total amount. A real system would get price from Inventory Service.
        order.setTotalAmount(new BigDecimal("50.00").multiply(new BigDecimal(request.getQuantity())));

        Order savedOrder = orderRepository.save(order);

        // --- Step 3: Place temporary hold in Redis [cite: 16] ---
        // This key will automatically expire after HOLD_DURATION
        String holdKey = "ticket_hold:" + savedOrder.getId();
        String holdValue = savedOrder.getEventId() + ":" + savedOrder.getQuantity();

        redisTemplate.opsForValue().set(holdKey, holdValue, HOLD_DURATION);

        System.out.println("Placed hold in Redis for order: " + savedOrder.getId() + " for " + HOLD_DURATION.toMinutes() + " minutes.");

        // --- Step 4: Return response to user ---
        // The user would now be redirected to a payment page.
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
            throw new RuntimeException("Order is already finalized. Current state: " + order.getStatus());
        }

        // 3. Check if the order is PENDING (it should be)
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order is not in PENDING state. Current state: " + order.getStatus());
        }

        // 4. Check if the hold in Redis still exists
        String holdKey = "ticket_hold:" + order.getId();
        Boolean holdExists = redisTemplate.hasKey(holdKey);

        if (holdExists == null || !holdExists) {
            // Hold has EXPIRED! The payment fails.
            System.err.println("Hold expired for order: " + orderId);

            // Set order status to EXPIRED
            order.setStatus(OrderStatus.EXPIRED);
            orderRepository.save(order);

            // --- THIS IS THE NEW LINE ---
            // Tell the inventory-service to release the tickets
            releaseTicketsInInventory(order.getEventId(), order.getQuantity());
            // --------------------------

            throw new RuntimeException("Hold expired. Payment failed.");
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