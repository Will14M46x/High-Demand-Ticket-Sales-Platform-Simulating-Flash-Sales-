package com.ticketsale.booking_service.controller;

import com.ticketsale.booking_service.dto.BookingRequest;
import com.ticketsale.booking_service.dto.OrderResponse;
import com.ticketsale.booking_service.model.OrderStatus;
import com.ticketsale.booking_service.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/booking")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @PostMapping
    public ResponseEntity<OrderResponse> createBooking(@RequestBody BookingRequest request) {
        try {
            OrderResponse response = bookingService.createBooking(request);
            // 201 Created status
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

            // --- ADD THIS NEW CATCH BLOCK ---
        } catch (SecurityException e) {
            // User was not admitted
            OrderResponse errorResponse = OrderResponse.builder()
                    .message(e.getMessage())
                    .build();
            // 403 Forbidden is the correct code for "I know who you are, but you can't do this"
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);

        } catch (RuntimeException e) {
            // e.g., if inventory service returned 409 Conflict (no stock)
            OrderResponse errorResponse = OrderResponse.builder()
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        }
    }

    /**
     * POST /api/booking/{orderId}/confirm
     * Simulates a successful payment confirmation from a payment gateway.
     */
    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<OrderResponse> confirmBooking(@PathVariable Long orderId) {

        // --- ADD A TRY-CATCH BLOCK ---
        try {
            OrderResponse response = bookingService.confirmPayment(orderId);

            if (response.getStatus() == OrderStatus.PAID) {
                return ResponseEntity.ok(response);
            } else {
                // This handles the EXPIRED case
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }
        } catch (Exception e) {
            // This will catch any other error (like Redis down)
            // and prevent the "silent rollback"
            OrderResponse errorResponse = OrderResponse.builder()
                    .orderId(orderId)
                    .message("Failed to confirm payment: " + e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


}