package la.meleva.fundraiser.campaign.dto;

import la.meleva.fundraiser.campaign.CampaignPrize;

import java.time.Instant;

public record PrizeResponse(
        Long id,
        CampaignPrize.PrizeType type,
        String description,
        long valueCents,
        String currency,
        int quantity,
        CampaignPrize.PrizeStatus status,
        int sortOrder,
        Instant createdAt
) {
    public static PrizeResponse from(CampaignPrize p) {
        return new PrizeResponse(
                p.getId(),
                p.getType(),
                p.getDescription(),
                p.getValueCents(),
                p.getCurrency(),
                p.getQuantity(),
                p.getStatus(),
                p.getSortOrder(),
                p.getCreatedAt()
        );
    }
}
