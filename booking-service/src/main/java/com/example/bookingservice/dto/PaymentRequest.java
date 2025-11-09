package com.example.bookingservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {
    private String holdId;
    private Long orderId;
    private double amount;
    private String paymentMethod;
    private String cardNumber;
    private String cvv;
    private String expiryDate;
}
