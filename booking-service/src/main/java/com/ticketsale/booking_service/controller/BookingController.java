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

    /**
     * POST /api/booking/{orderId}/confirm
     * Simulates a successful payment confirmation from a payment gateway.
     */
    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<OrderResponse> confirmBooking(@PathVariable Long orderId) {
        // We no longer need a try-catch block
        OrderResponse response = bookingService.confirmPayment(orderId);

        // Check the status of the order response
        if (response.getStatus() == OrderStatus.PAID) {
            // It was a success!
            return ResponseEntity.ok(response);
        } else {
            // It was a failure (e.g., EXPIRED or another bad state)
            // Return 409 Conflict with the error message from the service
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
    }


}