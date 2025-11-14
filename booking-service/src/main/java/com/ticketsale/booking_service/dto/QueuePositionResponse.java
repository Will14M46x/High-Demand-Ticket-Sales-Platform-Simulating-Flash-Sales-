package com.ticketsale.booking_service.dto;

import lombok.Data;

// This DTO is used to deserialize the response from the waiting-room-service
// It must match the fields in the waiting-room's QueuePositionResponse
@Data
public class QueuePositionResponse {
    private String userId;
    private Integer position;
    private String estimatedWaitTime;
}