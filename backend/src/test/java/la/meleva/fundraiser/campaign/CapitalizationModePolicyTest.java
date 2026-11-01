package la.meleva.fundraiser.campaign;

import la.meleva.fundraiser.compliance.GateType;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CapitalizationModePolicyTest {

    @Test
    void capitalizationAllowsCashAndFullCapabilities() {
        var mode = CampaignMode.CAPITALIZATION_PRIZE_TITLE;
        assertThat(mode.isDemo()).isFalse();
        assertThat(mode.allowsCashPrizes()).isTrue();
        assertThat(mode.allowsRealPayments()).isTrue();
        assertThat(mode.requiresAuthorization()).isTrue();

        var policy = CapabilityPolicy.forMode(mode);
        assertThat(policy.can(CapabilityPolicy.Capability.COLLECT_PAYMENTS)).isTrue();
        assertThat(policy.can(CapabilityPolicy.Capability.AWARD_CASH_PRIZES)).isTrue();
        assertThat(policy.can(CapabilityPolicy.Capability.AWARD_GOODS_PRIZES)).isTrue();
    }

    @Test
    void otherModesKeepTheirRestrictions() {
        assertThat(CampaignMode.DEMO.allowsCashPrizes()).isFalse();
        assertThat(CampaignMode.COMMERCIAL_PROMOTION.allowsCashPrizes()).isFalse();
        assertThat(CampaignMode.COMMERCIAL_PROMOTION.allowsRealPayments()).isFalse();
        assertThat(CapabilityPolicy.forMode(CampaignMode.COMMERCIAL_PROMOTION)
                .can(CapabilityPolicy.Capability.COLLECT_PAYMENTS)).isFalse();
    }

    @Test
    void capitalizationRequiresEveryGate() {
        assertThat(AdminGateMatrix.requiredGates(CampaignMode.CAPITALIZATION_PRIZE_TITLE))
                .containsExactlyInAnyOrder(GateType.G_LEGAL, GateType.G_PSP, GateType.G_FINANCE,
                        GateType.G_SECURITY, GateType.G_OPERATIONS, GateType.G_LAUNCH);
    }
}
