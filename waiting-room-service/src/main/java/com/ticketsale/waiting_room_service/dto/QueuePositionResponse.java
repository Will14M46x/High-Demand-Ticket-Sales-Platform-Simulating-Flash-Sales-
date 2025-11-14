package com.ticketsale.waiting_room_service.dto; // <-- New package

import lombok.Builder;
import lombok.Data;

@Data
@Builder
// This code is based on your provided file
public class QueuePositionResponse {
    private String userId;
    private Integer position;
    private String estimatedWaitTime;
}