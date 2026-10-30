package la.meleva.fundraiser.campaign.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UpdateCampaignRequest(
        @Size(max = 255)
        String title,

        @Size(max = 100)
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "Slug deve conter apenas letras minúsculas, números e hífens")
        String slug,

        @Size(max = 10000)
        String description,

        Instant startsAt,
        Instant endsAt
) {}
