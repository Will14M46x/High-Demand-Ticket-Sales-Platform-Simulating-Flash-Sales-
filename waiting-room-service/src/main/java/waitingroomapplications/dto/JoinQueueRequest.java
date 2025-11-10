package waitingroomapplications.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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