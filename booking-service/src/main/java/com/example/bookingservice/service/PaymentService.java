package com.example.bookingservice.service;

import com.example.bookingservice.dto.PaymentRequest;
import com.example.bookingservice.dto.PaymentResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Mock Payment Service
 * In a real implementation, this would integrate with an actual payment gateway
 * like Stripe, PayPal, etc.
 */
@Service
public class PaymentService {

    /**
     * Process payment (mock implementation)
     * In production, this would make API calls to a real payment gateway
     */
    public PaymentResponse processPayment(PaymentRequest request) {
        try {
            // Simulate payment processing delay
            Thread.sleep(1000);

            // Mock validation
            if (request.getAmount() <= 0) {
                return PaymentResponse.builder()
                        .paymentId(null)
                        .status("FAILED")
                        .message("Invalid payment amount")
                        .orderId(request.getOrderId())
                        .build();
            }

            if (request.getCardNumber() == null || request.getCardNumber().length() < 13) {
                return PaymentResponse.builder()
                        .paymentId(null)
                        .status("FAILED")
                        .message("Invalid card number")
                        .orderId(request.getOrderId())
                        .build();
            }

            // Simulate 90% success rate
            boolean success = Math.random() < 0.9;

            if (success) {
                String paymentId = "PAY_" + UUID.randomUUID().toString();
                return PaymentResponse.builder()
                        .paymentId(paymentId)
                        .status("SUCCESS")
                        .message("Payment processed successfully")
                        .orderId(request.getOrderId())
                        .build();
            } else {
                return PaymentResponse.builder()
                        .paymentId(null)
                        .status("FAILED")
                        .message("Payment declined by bank")
                        .orderId(request.getOrderId())
                        .build();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return PaymentResponse.builder()
                    .paymentId(null)
                    .status("FAILED")
                    .message("Payment processing interrupted")
                    .orderId(request.getOrderId())
                    .build();
        }
    }

    /**
     * Refund payment (mock implementation)
     */
    public boolean refundPayment(String paymentId, double amount) {
        // Mock refund logic
        // In production, this would call the payment gateway's refund API
        return true;
    }
}
