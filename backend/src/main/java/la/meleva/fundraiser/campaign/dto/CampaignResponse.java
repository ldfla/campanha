package la.meleva.fundraiser.campaign.dto;

import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignStateMachine;
import la.meleva.fundraiser.campaign.CampaignStatus;
import la.meleva.fundraiser.campaign.CapabilityPolicy;

import java.time.Instant;
import java.util.Set;

public record CampaignResponse(
        String id,
        String title,
        String slug,
        String description,
        CampaignMode mode,
        CampaignStatus status,
        Set<CampaignStatus> allowedTransitions,
        Set<CapabilityPolicy.Capability> capabilities,
        Instant startsAt,
        Instant endsAt,
        String timezone,
        int numberMin,
        int numberMax,
        int totalNumbers,
        Instant createdAt,
        Instant updatedAt
) {
    public static CampaignResponse from(Campaign c) {
        return new CampaignResponse(
                c.getPublicId(),
                c.getTitle(),
                c.getSlug(),
                c.getDescription(),
                c.getMode(),
                c.getStatus(),
                CampaignStateMachine.allowedTransitions(c.getStatus()),
                c.capabilities().capabilities(),
                c.getStartsAt(),
                c.getEndsAt(),
                c.getTimezone(),
                c.getNumberMin(),
                c.getNumberMax(),
                c.totalNumbers(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
