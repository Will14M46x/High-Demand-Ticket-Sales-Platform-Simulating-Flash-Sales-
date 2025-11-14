package com.ticketsale.waiting_room_service.dto; // <-- New package

import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
// This code is based on your provided file
public class AdmitBatchRequest {
    @Min(1)
    private Integer batchSize;
    @Min(1)
    private Long eventId;
}