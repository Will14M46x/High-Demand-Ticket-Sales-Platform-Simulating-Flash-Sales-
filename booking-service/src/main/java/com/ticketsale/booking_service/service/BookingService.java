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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class BookingService {

    // ... (All @Autowired and @Value fields are the same) ...
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;
    @Value("${waitingroom.service.url}")
    private String waitingRoomServiceUrl;

    private static final Duration HOLD_DURATION = Duration.ofMinutes(5);


    private boolean isUserAdmitted(String userId, Long eventId) {
        String checkPositionUrl = String.format(
                "%s/position/%s?eventId=%d",
                waitingRoomServiceUrl,
                userId,
                eventId
        );
        try {
            ResponseEntity<QueuePositionResponse> response = restTemplate.getForEntity(
                    checkPositionUrl,
                    QueuePositionResponse.class
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Integer position = response.getBody().getPosition();
                return position != null && position == 0;
            }
            return false;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    // --- THIS IS THE CHANGED METHOD SIGNATURE ---
    @Transactional
    public OrderResponse createBooking(BookingRequest request, Long userId) {

        // --- STEP 1: CHECK WAITING ROOM ---
        // We now use the secure userId
        String userIdStr = String.valueOf(userId);

        if (!isUserAdmitted(userIdStr, request.getEventId())) {
            System.err.println("User " + userIdStr + " REJECTED. Not admitted from waiting room.");
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
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Failed to reserve tickets (e.g., insufficient stock or concurrency conflict).");
        } catch (Exception e) {
            throw new RuntimeException("Error communicating with inventory service.");
        }

        // --- Step 3: Create PENDING order (NOW USES SECURE 'userId') ---
        Order order = new Order();
        order.setUserId(userId); // <-- Using the secure ID
        order.setEventId(request.getEventId());
        order.setQuantity(request.getQuantity());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setTotalAmount(new BigDecimal("50.00").multiply(new BigDecimal(request.getQuantity())));
        Order savedOrder = orderRepository.save(order);

        // ... (Rest of the method is the same) ...
        String holdKey = "ticket_hold:" + savedOrder.getId();
        String holdValue = savedOrder.getEventId() + ":" + savedOrder.getQuantity();
        redisTemplate.opsForValue().set(holdKey, holdValue, HOLD_DURATION);

        return mapToOrderResponse(savedOrder, "Booking pending. Please complete payment within 5 minutes.");
    }

    // --- (confirmPayment, releaseTicketsInInventory, and mapToOrderResponse methods are all unchanged) ---

    @Transactional
    public OrderResponse confirmPayment(Long orderId) {
        // ... (No changes to this method) ...
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.EXPIRED) {
            return mapToOrderResponse(order, "Order is already finalized. Current state: " + order.getStatus());
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            return mapToOrderResponse(order, "Order is not in PENDING state. Current state: " + order.getStatus());
        }
        String holdKey = "ticket_hold:" + order.getId();
        Boolean holdExists = redisTemplate.hasKey(holdKey);
        if (holdExists == null || !holdExists) {
            order.setStatus(OrderStatus.EXPIRED);
            Order expiredOrder = orderRepository.save(order);
            releaseTicketsInInventory(order.getEventId(), order.getQuantity());
            return mapToOrderResponse(expiredOrder, "Hold expired. Payment failed.");
        }
        redisTemplate.delete(holdKey);
        order.setStatus(OrderStatus.PAID);
        Order confirmedOrder = orderRepository.save(order);
        return mapToOrderResponse(confirmedOrder, "Payment successful. Order confirmed.");
    }

    private void releaseTicketsInInventory(Long eventId, int quantity) {
        // ... (No changes to this method) ...
        String releaseUrl = inventoryServiceUrl + "/" + eventId + "/release?quantity=" + quantity;
        try {
            restTemplate.postForEntity(releaseUrl, null, String.class);
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Failed to release tickets in inventory. " + e.getMessage());
        }
    }

    private OrderResponse mapToOrderResponse(Order order, String message) {
        // ... (No changes to this method) ...
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