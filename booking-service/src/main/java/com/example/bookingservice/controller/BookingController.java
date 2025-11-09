package com.example.bookingservice.controller;

import com.example.bookingservice.dto.*;
import com.example.bookingservice.model.Order;
import com.example.bookingservice.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Create a new booking (reserve tickets)
     * POST /api/bookings
     */
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@RequestBody BookingRequest request) {
        BookingResponse response = bookingService.createBooking(request);
        
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Complete booking with payment
     * POST /api/bookings/payment
     */
    @PostMapping("/payment")
    public ResponseEntity<PaymentResponse> completePayment(@RequestBody PaymentRequest request) {
        PaymentResponse response = bookingService.completeBooking(request);
        
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get order details
     * GET /api/bookings/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable Long orderId) {
        Order order = bookingService.getOrder(orderId);
        
        if (order != null) {
            return ResponseEntity.ok(order);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all orders for a user
     * GET /api/bookings/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getUserOrders(@PathVariable Long userId) {
        List<Order> orders = bookingService.getUserOrders(userId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Cancel a booking
     * DELETE /api/bookings/{orderId}
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<String> cancelBooking(@PathVariable Long orderId) {
        boolean cancelled = bookingService.cancelBooking(orderId);
        
        if (cancelled) {
            return ResponseEntity.ok("Booking cancelled successfully");
        } else {
            return ResponseEntity.badRequest().body("Failed to cancel booking");
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Booking Service is running");
    }
}
