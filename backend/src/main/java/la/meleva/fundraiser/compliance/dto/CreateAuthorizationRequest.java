package la.meleva.fundraiser.compliance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateAuthorizationRequest(
        @NotBlank @Size(max = 255) String issuer,
        @NotBlank @Size(max = 100) String certificateNo,
        Instant validFrom,
        Instant validTo,
        @Size(max = 1024) String documentUri) {}
