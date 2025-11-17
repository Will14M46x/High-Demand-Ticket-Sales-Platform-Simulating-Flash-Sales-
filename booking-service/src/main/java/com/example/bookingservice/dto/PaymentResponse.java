package com.example.bookingservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private String paymentId;
    private String status; // "SUCCESS", "FAILED", "PENDING"
    private String message;
    private Long orderId;
}
