package la.meleva.fundraiser.participation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record CreateParticipationRequest(
        @NotBlank @Pattern(regexp = "^[0-9a-fA-F-]{36}$") String idempotencyKey,
        @Min(1) @Max(100) int count,
        @NotBlank @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$") String entityId,
        @Nullable @Size(max = 20) String referralCode) {}
