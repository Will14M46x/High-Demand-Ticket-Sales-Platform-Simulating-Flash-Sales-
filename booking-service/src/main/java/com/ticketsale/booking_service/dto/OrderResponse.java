package com.ticketsale.booking_service.dto;

import com.ticketsale.booking_service.model.OrderStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// This is what we send back to the user after they create a booking
@Data
@Builder // A useful pattern for creating DTOs
public class OrderResponse {
    private Long orderId;
    private Long userId;
    private Long eventId;
    private Integer quantity;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private String message;
}