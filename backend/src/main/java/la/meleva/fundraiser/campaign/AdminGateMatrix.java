package la.meleva.fundraiser.campaign;

import la.meleva.fundraiser.compliance.GateType;
import la.meleva.fundraiser.shared.ErrorCode;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class AdminGateMatrix {

    private static final Map<CampaignMode, Set<GateType>> REQUIRED_GATES = Map.of(
            CampaignMode.DEMO, Set.of(),
            CampaignMode.COMMERCIAL_PROMOTION, EnumSet.of(
                    GateType.G_LEGAL,
                    GateType.G_SECURITY,
                    GateType.G_OPERATIONS,
                    GateType.G_LAUNCH
            ),
            CampaignMode.PHILANTHROPIC_AUTHORIZED, EnumSet.allOf(GateType.class),
            CampaignMode.CAPITALIZATION_PRIZE_TITLE, EnumSet.allOf(GateType.class)
    );

    private AdminGateMatrix() {}

    public static Set<GateType> requiredGates(CampaignMode mode) {
        return REQUIRED_GATES.getOrDefault(mode, Set.of());
    }

    public static void requireGatesForTransition(CampaignMode mode, CampaignStatus targetStatus) {
        if (mode.isDemo()) return;

        if (targetStatus == CampaignStatus.SCHEDULED || targetStatus == CampaignStatus.OPEN) {
            Set<GateType> required = requiredGates(mode);
            if (!required.isEmpty()) {
                throw ErrorCode.CAMPAIGN_GATES_INCOMPLETE.toException();
            }
        }
    }

    public static boolean isGateRequired(CampaignMode mode, GateType gate) {
        return requiredGates(mode).contains(gate);
    }
}
