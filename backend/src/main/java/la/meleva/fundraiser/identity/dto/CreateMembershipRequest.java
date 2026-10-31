package la.meleva.fundraiser.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import la.meleva.fundraiser.identity.UserRole;

public record CreateMembershipRequest(@NotBlank String userPublicId, @NotBlank String organizationPublicId, @NotNull UserRole role) {}
