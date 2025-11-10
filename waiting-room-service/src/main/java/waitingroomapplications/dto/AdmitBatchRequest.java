package waitingroomapplications.dto;

import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdmitBatchRequest {
    @Min(1)
    private Integer batchSize;

    @Min(1)
    private Long eventId;
}