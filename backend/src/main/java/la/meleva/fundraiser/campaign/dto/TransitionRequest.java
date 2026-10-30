package la.meleva.fundraiser.campaign.dto;

import jakarta.validation.constraints.NotNull;
import la.meleva.fundraiser.campaign.CampaignStatus;

public record TransitionRequest(
        @NotNull(message = "Status de destino é obrigatório")
        CampaignStatus targetStatus
) {}
