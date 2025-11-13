package com.ticketsale.booking_service.controller;

import com.ticketsale.booking_service.dto.BookingRequest;
import com.ticketsale.booking_service.dto.OrderResponse;
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

    /**
     * POST /api/booking
     * Creates a new booking reservation (places a hold) [cite: 15]
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createBooking(@RequestBody BookingRequest request) {
        try {
            OrderResponse response = bookingService.createBooking(request);
            // 201 Created status
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            // e.g., if inventory service returned 409 Conflict (no stock)
            OrderResponse errorResponse = OrderResponse.builder()
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        }
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<OrderResponse> confirmBooking(@PathVariable Long orderId) {
        try {
            OrderResponse response = bookingService.confirmPayment(orderId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // This will catch "Order not found", "Not pending", or "Hold expired"
            OrderResponse errorResponse = OrderResponse.builder()
                    .orderId(orderId)
                    .message(e.getMessage())
                    .build();
            // 409 Conflict is a good status for a failed state change
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        }
    }


}