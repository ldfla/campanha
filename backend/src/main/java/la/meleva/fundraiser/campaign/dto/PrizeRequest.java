package la.meleva.fundraiser.campaign.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import la.meleva.fundraiser.campaign.CampaignPrize;

public record PrizeRequest(
        @NotNull(message = "Tipo do prêmio é obrigatório")
        CampaignPrize.PrizeType type,

        @NotBlank(message = "Descrição é obrigatória")
        @Size(max = 500)
        String description,

        @Min(value = 0, message = "Valor não pode ser negativo")
        long valueCents,

        @Min(value = 1, message = "Quantidade mínima é 1")
        int quantity,

        @Min(value = 0)
        int sortOrder
) {}
