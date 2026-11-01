package la.meleva.fundraiser.drawing;

import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.shared.ErrorCode;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SurprisePrizePolicyTest {

    @Test void allowsSimulatedCashInDemoAndCapitalizationTitle() {
        assertThat(SurprisePrizePolicy.requireAllowed(CampaignMode.DEMO))
                .isEqualTo(SurprisePrizePolicy.PrizeType.SIMULATED_CASH);
        assertThat(SurprisePrizePolicy.requireAllowed(CampaignMode.CAPITALIZATION_PRIZE_TITLE))
                .isEqualTo(SurprisePrizePolicy.PrizeType.SIMULATED_CASH);
    }

    @Test void refusesCashPrizesForNonAuthorizedModalities() {
        for (var mode : CampaignMode.values()) {
            if (mode == CampaignMode.DEMO || mode == CampaignMode.CAPITALIZATION_PRIZE_TITLE) continue;
            assertThatThrownBy(() -> SurprisePrizePolicy.requireAllowed(mode))
                    .isInstanceOfSatisfying(ErrorCode.BusinessException.class,
                            ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.CAMPAIGN_NOT_AUTHORIZED));
        }
    }

    @Test void rejectsMissingModality() {
        assertThatThrownBy(() -> SurprisePrizePolicy.requireAllowed(null))
                .isInstanceOfSatisfying(ErrorCode.BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }
}
