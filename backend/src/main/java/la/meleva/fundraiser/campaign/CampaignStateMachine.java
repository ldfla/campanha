package la.meleva.fundraiser.campaign;

import la.meleva.fundraiser.shared.ErrorCode;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class CampaignStateMachine {

    private static final Map<CampaignStatus, Set<CampaignStatus>> TRANSITIONS = Map.ofEntries(
            Map.entry(CampaignStatus.DRAFT, EnumSet.of(
                    CampaignStatus.LEGAL_REVIEW, CampaignStatus.CANCELLED)),
            Map.entry(CampaignStatus.LEGAL_REVIEW, EnumSet.of(
                    CampaignStatus.AUTHORIZED, CampaignStatus.DRAFT, CampaignStatus.CANCELLED)),
            Map.entry(CampaignStatus.AUTHORIZED, EnumSet.of(
                    CampaignStatus.SCHEDULED, CampaignStatus.CANCELLED)),
            Map.entry(CampaignStatus.SCHEDULED, EnumSet.of(
                    CampaignStatus.OPEN, CampaignStatus.CANCELLED)),
            Map.entry(CampaignStatus.OPEN, EnumSet.of(
                    CampaignStatus.LOCKED, CampaignStatus.SUSPENDED)),
            Map.entry(CampaignStatus.LOCKED, EnumSet.of(
                    CampaignStatus.DRAW_PENDING)),
            Map.entry(CampaignStatus.DRAW_PENDING, EnumSet.of(
                    CampaignStatus.DRAWN)),
            Map.entry(CampaignStatus.DRAWN, EnumSet.of(
                    CampaignStatus.AWARDS_PENDING)),
            Map.entry(CampaignStatus.AWARDS_PENDING, EnumSet.of(
                    CampaignStatus.CLOSED)),
            Map.entry(CampaignStatus.SUSPENDED, EnumSet.of(
                    CampaignStatus.OPEN, CampaignStatus.CANCELLED)),
            Map.entry(CampaignStatus.CLOSED, Set.of()),
            Map.entry(CampaignStatus.CANCELLED, Set.of())
    );

    private CampaignStateMachine() {}

    public static void validate(CampaignStatus from, CampaignStatus to) {
        Set<CampaignStatus> allowed = TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw ErrorCode.CAMPAIGN_INVALID_TRANSITION.toException();
        }
    }

    public static Set<CampaignStatus> allowedTransitions(CampaignStatus from) {
        return TRANSITIONS.getOrDefault(from, Set.of());
    }

    public static boolean canTransition(CampaignStatus from, CampaignStatus to) {
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }
}
