package com.ticketsale.booking_service.dto;

import lombok.Data;

// This is what the user sends to create a booking
@Data
public class BookingRequest {
    private Long userId;   // In a real system, this would come from the Auth service
    private Long eventId;
    private Integer quantity;
    // We'd also have payment info here (e.g., a payment token)
}