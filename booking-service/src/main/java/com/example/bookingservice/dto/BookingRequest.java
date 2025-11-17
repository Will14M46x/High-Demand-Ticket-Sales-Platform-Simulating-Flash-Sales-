package com.example.bookingservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRequest {
    private Long eventId;
    private Long userId;
    private int quantity;
}
