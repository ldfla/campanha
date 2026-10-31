package la.meleva.fundraiser.drawing;

import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.shared.ErrorCode;

/**
 * Regra de modalidade para os prêmios-surpresa. O prêmio é sempre em dinheiro/simulado; só é
 * admitido em {@link CampaignMode#DEMO}. Promoção comercial não pode oferecer dinheiro.
 */
public final class SurprisePrizePolicy {

    public enum PrizeType { SIMULATED_CASH, GOODS }

    private SurprisePrizePolicy() {}

    public static PrizeType requireAllowed(CampaignMode mode) {
        if (mode == null) throw ErrorCode.VALIDATION_ERROR.toException();
        if (mode != CampaignMode.DEMO) throw ErrorCode.CAMPAIGN_NOT_AUTHORIZED.toException();
        return PrizeType.SIMULATED_CASH;
    }
}
