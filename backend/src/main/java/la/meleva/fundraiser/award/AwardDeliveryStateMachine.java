package la.meleva.fundraiser.award;

import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import la.meleva.fundraiser.shared.ErrorCode;

/** Transições permitidas; autorização e provas são exigidas pelo serviço de aplicação. */
public final class AwardDeliveryStateMachine {
    private static final Map<AwardDeliveryStatus,Set<AwardDeliveryStatus>> TRANSITIONS = Map.of(
            AwardDeliveryStatus.PENDING_VERIFICATION, EnumSet.of(AwardDeliveryStatus.VALIDATED,AwardDeliveryStatus.DISPUTED),
            AwardDeliveryStatus.VALIDATED, EnumSet.of(AwardDeliveryStatus.SCHEDULED,AwardDeliveryStatus.DISPUTED),
            AwardDeliveryStatus.SCHEDULED, EnumSet.of(AwardDeliveryStatus.DELIVERED,AwardDeliveryStatus.DISPUTED),
            AwardDeliveryStatus.DELIVERED, EnumSet.of(AwardDeliveryStatus.DISPUTED),
            AwardDeliveryStatus.DISPUTED, Set.of());

    private AwardDeliveryStateMachine() {}

    public static void validate(AwardDeliveryStatus current,AwardDeliveryStatus target) {
        Objects.requireNonNull(current,"current"); Objects.requireNonNull(target,"target");
        if(!TRANSITIONS.get(current).contains(target)) throw ErrorCode.AWARD_INVALID_TRANSITION.toException();
    }
}
