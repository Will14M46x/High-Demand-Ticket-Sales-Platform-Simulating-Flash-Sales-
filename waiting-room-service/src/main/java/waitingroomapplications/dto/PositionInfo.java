package waitingroomapplications.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PositionInfo {
    private PositionStatus status;
    private Integer position; // 1-based position when IN_QUEUE, 0 when ADMITTED, null when NOT_FOUND
}


