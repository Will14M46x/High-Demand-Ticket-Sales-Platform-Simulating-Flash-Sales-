package waitingroomapplications.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QueueStatusResponse {
    private Long totalWaiting;
    private Long totalAdmitted;
    private String estimatedWaitTime;
}
