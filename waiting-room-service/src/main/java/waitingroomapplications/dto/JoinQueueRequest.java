package waitingroomapplications.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JoinQueueRequest {
    @NotBlank
    private String userId;

    @Min(1)
    private Long eventId;

    @Min(1)
    private Integer requestedQuantity;
}