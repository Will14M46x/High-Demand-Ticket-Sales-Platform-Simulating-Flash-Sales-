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
    public ResponseEntity<OrderResponse> createBooking(
            @RequestBody BookingRequest request,
            // --- THIS IS THE CHANGE ---
            // We get the userId from the token, not the body
            @RequestAttribute("userId") Long userId
    ) {
        try {
            // --- WE PASS THE SECURE 'userId' TO THE SERVICE ---
            OrderResponse response = bookingService.createBooking(request, userId);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (SecurityException e) {
            OrderResponse errorResponse = OrderResponse.builder().message(e.getMessage()).build();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);

        } catch (RuntimeException e) {
            OrderResponse errorResponse = OrderResponse.builder().message(e.getMessage()).build();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        }
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<OrderResponse> confirmBooking(@PathVariable Long orderId) {

        try {
            OrderResponse response = bookingService.confirmPayment(orderId);
            if (response.getStatus() == OrderStatus.PAID) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }
        } catch (Exception e) {
            OrderResponse errorResponse = OrderResponse.builder()
                    .orderId(orderId)
                    .message("Failed to confirm payment: " + e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}