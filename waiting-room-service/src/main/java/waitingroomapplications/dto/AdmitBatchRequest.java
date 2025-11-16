package waitingroomapplications.dto;

import javax.validation.constraints.Min;
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