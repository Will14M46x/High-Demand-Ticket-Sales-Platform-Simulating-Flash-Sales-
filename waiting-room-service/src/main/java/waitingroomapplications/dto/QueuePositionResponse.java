package waitingroomapplications.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QueuePositionResponse {
    private String userId;
    private Integer position;
    private String estimatedWaitTime;
}
