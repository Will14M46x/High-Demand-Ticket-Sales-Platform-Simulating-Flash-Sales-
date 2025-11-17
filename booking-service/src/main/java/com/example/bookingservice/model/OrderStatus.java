package com.example.bookingservice.model;

public enum OrderStatus {
    PENDING,        // Hold created, waiting for payment
    PROCESSING,     // Payment being processed
    COMPLETED,      // Payment successful, tickets confirmed
    FAILED,         // Payment failed
    EXPIRED,        // Hold expired before payment
    CANCELLED       // User cancelled
}
