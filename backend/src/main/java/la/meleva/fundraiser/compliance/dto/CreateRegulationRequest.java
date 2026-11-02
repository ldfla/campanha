package la.meleva.fundraiser.compliance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateRegulationRequest(
        @NotBlank @Pattern(regexp = "^[a-fA-F0-9]{64}$") String documentSha256,
        @Size(max = 1024) String documentUri,
        boolean surpriseRequired) {}
