package la.meleva.fundraiser.drawing;

import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.shared.ErrorCode;

/**
 * Regra de modalidade para os prêmios-surpresa. O prêmio é em dinheiro/simulado; admitido em
 * {@link CampaignMode#DEMO} e no título de capitalização premiável autorizado (ADR-0007).
 */
public final class SurprisePrizePolicy {

    public enum PrizeType { SIMULATED_CASH, GOODS }

    private SurprisePrizePolicy() {}

    public static PrizeType requireAllowed(CampaignMode mode) {
        if (mode == null) throw ErrorCode.VALIDATION_ERROR.toException();
        if (mode != CampaignMode.DEMO && mode != CampaignMode.CAPITALIZATION_PRIZE_TITLE) {
            throw ErrorCode.CAMPAIGN_NOT_AUTHORIZED.toException();
        }
        return PrizeType.SIMULATED_CASH;
    }
}
