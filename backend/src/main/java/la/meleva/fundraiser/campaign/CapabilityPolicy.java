package la.meleva.fundraiser.campaign;

import java.util.EnumSet;
import java.util.Set;

public record CapabilityPolicy(CampaignMode mode, Set<Capability> capabilities) {

    public enum Capability {
        ACCEPT_PARTICIPATIONS,
        COLLECT_PAYMENTS,
        AWARD_CASH_PRIZES,
        AWARD_GOODS_PRIZES,
        PUBLISH_CAMPAIGN,
        SHOW_RANKINGS,
        SHOW_SURPRISE_NUMBERS,
        EXECUTE_DRAWING,
        DELIVER_AWARDS
    }

    public static CapabilityPolicy forMode(CampaignMode mode) {
        return switch (mode) {
            case DEMO -> new CapabilityPolicy(mode, EnumSet.of(
                    Capability.ACCEPT_PARTICIPATIONS,
                    Capability.AWARD_GOODS_PRIZES,
                    Capability.SHOW_RANKINGS,
                    Capability.SHOW_SURPRISE_NUMBERS,
                    Capability.EXECUTE_DRAWING
            ));
            case COMMERCIAL_PROMOTION -> new CapabilityPolicy(mode, EnumSet.of(
                    Capability.ACCEPT_PARTICIPATIONS,
                    Capability.AWARD_GOODS_PRIZES,
                    Capability.PUBLISH_CAMPAIGN,
                    Capability.SHOW_RANKINGS,
                    Capability.SHOW_SURPRISE_NUMBERS,
                    Capability.EXECUTE_DRAWING,
                    Capability.DELIVER_AWARDS
            ));
            case PHILANTHROPIC_AUTHORIZED -> new CapabilityPolicy(mode, EnumSet.of(
                    Capability.ACCEPT_PARTICIPATIONS,
                    Capability.COLLECT_PAYMENTS,
                    Capability.AWARD_GOODS_PRIZES,
                    Capability.PUBLISH_CAMPAIGN,
                    Capability.SHOW_RANKINGS,
                    Capability.SHOW_SURPRISE_NUMBERS,
                    Capability.EXECUTE_DRAWING,
                    Capability.DELIVER_AWARDS
            ));
        };
    }

    public boolean can(Capability capability) {
        return capabilities.contains(capability);
    }

    public void require(Capability capability) {
        if (!can(capability)) {
            throw new CampaignOperationBlockedException(
                    "Operação não permitida no modo " + mode + ": " + capability
            );
        }
    }
}
