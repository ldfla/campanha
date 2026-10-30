package la.meleva.fundraiser.campaign.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import la.meleva.fundraiser.campaign.CampaignMode;

public record CreateCampaignRequest(
        @NotBlank(message = "Título é obrigatório")
        @Size(max = 255)
        String title,

        @NotBlank(message = "Slug é obrigatório")
        @Size(max = 100)
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "Slug deve conter apenas letras minúsculas, números e hífens")
        String slug,

        @Size(max = 10000)
        String description,

        @NotNull(message = "Modo da campanha é obrigatório")
        CampaignMode mode
) {}
