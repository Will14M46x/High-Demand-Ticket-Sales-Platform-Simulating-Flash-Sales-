package com.ticketsale.booking_service.model;

// This Enum defines the possible states for an order
public enum OrderStatus {
    PENDING,        // Initial state, during hold [cite: 16]
    PAYMENT_FAILED, // Payment was not successful
    PAID,           // Payment successful, order confirmed [cite: 17]
    EXPIRED         // Hold time ran out [cite: 18]
}