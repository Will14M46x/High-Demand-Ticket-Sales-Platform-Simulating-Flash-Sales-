package com.example.bookingservice.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ScheduledTaskService {

    private final BookingService bookingService;

    public ScheduledTaskService(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Cleanup expired holds every minute
     */
    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    public void cleanupExpiredHolds() {
        bookingService.cleanupExpiredHolds();
    }
}
