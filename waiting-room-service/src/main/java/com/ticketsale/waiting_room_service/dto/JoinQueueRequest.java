package com.ticketsale.waiting_room_service.dto; // <-- New package

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
// This code is based on your provided file
public class JoinQueueRequest {
    @NotBlank
    private String userId;
    @Min(1)
    private Long eventId;
    @Min(1)
    private Integer requestedQuantity;
}