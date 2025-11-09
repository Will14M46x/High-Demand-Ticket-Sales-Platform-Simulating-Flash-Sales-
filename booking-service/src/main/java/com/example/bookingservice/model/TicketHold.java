package com.example.bookingservice.model;

import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketHold implements Serializable {
    private String holdId;
    private Long eventId;
    private Long userId;
    private int quantity;
    private double totalPrice;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean isActive;
}
