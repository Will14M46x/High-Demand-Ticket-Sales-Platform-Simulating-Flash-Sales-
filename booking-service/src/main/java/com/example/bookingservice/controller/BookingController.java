package com.example.bookingservice.controller;

import com.example.bookingservice.dto.*;
import com.example.bookingservice.model.Order;
import com.example.bookingservice.service.BookingService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {
    
    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "booking-service");
        return ResponseEntity.ok(response);
    }

    /**
     * Create a new booking (reserve tickets) - requires authentication
     * POST /api/bookings
     */
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@RequestBody BookingRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        String email = (String) httpRequest.getAttribute("email");
        logger.info("User {} (ID: {}) creating booking for event: {}", email, userId, request.getEventId());
        
        // Set authenticated user ID in the booking request
        request.setUserId(userId);
        
        BookingResponse response = bookingService.createBooking(request);
        
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Complete booking with payment - requires authentication
     * POST /api/bookings/payment
     */
    @PostMapping("/payment")
    public ResponseEntity<PaymentResponse> completePayment(@RequestBody PaymentRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        String email = (String) httpRequest.getAttribute("email");
        logger.info("User {} (ID: {}) completing payment for order: {}", email, userId, request.getOrderId());
        
        PaymentResponse response = bookingService.completeBooking(request);
        
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get order details - requires authentication
     * GET /api/bookings/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable Long orderId, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        String email = (String) httpRequest.getAttribute("email");
        logger.debug("User {} (ID: {}) fetching order: {}", email, userId, orderId);
        
        Order order = bookingService.getOrder(orderId);
        
        // Ensure user can only access their own orders
        if (order != null && order.getUserId().equals(userId)) {
            return ResponseEntity.ok(order);
        } else if (order != null) {
            logger.warn("User {} attempted to access order {} belonging to user {}", userId, orderId, order.getUserId());
            return ResponseEntity.status(403).build(); // Forbidden
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all orders for the authenticated user
     * GET /api/bookings/user/me
     */
    @GetMapping("/user/me")
    public ResponseEntity<List<Order>> getMyOrders(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        String email = (String) httpRequest.getAttribute("email");
        logger.debug("User {} (ID: {}) fetching their orders", email, userId);
        
        List<Order> orders = bookingService.getUserOrders(userId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Cancel a booking - requires authentication
     * DELETE /api/bookings/{orderId}
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<String> cancelBooking(@PathVariable Long orderId, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        String email = (String) httpRequest.getAttribute("email");
        logger.info("User {} (ID: {}) cancelling order: {}", email, userId, orderId);
        
        // Verify order belongs to user before cancelling
        Order order = bookingService.getOrder(orderId);
        if (order != null && !order.getUserId().equals(userId)) {
            logger.warn("User {} attempted to cancel order {} belonging to user {}", userId, orderId, order.getUserId());
            return ResponseEntity.status(403).body("You can only cancel your own bookings");
        }
        
        boolean cancelled = bookingService.cancelBooking(orderId);
        
        if (cancelled) {
            return ResponseEntity.ok("Booking cancelled successfully");
        } else {
            return ResponseEntity.badRequest().body("Failed to cancel booking");
        }
    }
}
