package com.example.bookingservice.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {
    private String holdId;
    private Long orderId;
    private Long eventId;
    private int quantity;
    private double totalPrice;
    private LocalDateTime expiresAt;
    private String status;
    private String message;
}
