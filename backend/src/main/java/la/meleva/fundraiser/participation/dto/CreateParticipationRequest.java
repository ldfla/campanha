package la.meleva.fundraiser.participation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateParticipationRequest(
        @NotBlank @Pattern(regexp = "^[0-9a-fA-F-]{36}$") String idempotencyKey,
        @Min(1) @Max(100) int count) {}
