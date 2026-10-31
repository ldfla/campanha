package la.meleva.fundraiser.award;

import la.meleva.fundraiser.shared.ErrorCode;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AwardDeliveryStateMachineTest {
    @Test void validatesDeliverySequenceAndAllowsDispute() {
        AwardDeliveryStateMachine.validate(AwardDeliveryStatus.PENDING_VERIFICATION,AwardDeliveryStatus.VALIDATED);
        AwardDeliveryStateMachine.validate(AwardDeliveryStatus.VALIDATED,AwardDeliveryStatus.SCHEDULED);
        AwardDeliveryStateMachine.validate(AwardDeliveryStatus.SCHEDULED,AwardDeliveryStatus.DELIVERED);
        AwardDeliveryStateMachine.validate(AwardDeliveryStatus.DELIVERED,AwardDeliveryStatus.DISPUTED);
    }
    @Test void rejectsSkippingVerificationReopeningAndNullStates() {
        assertThatThrownBy(()->AwardDeliveryStateMachine.validate(AwardDeliveryStatus.PENDING_VERIFICATION,AwardDeliveryStatus.DELIVERED))
                .isInstanceOfSatisfying(ErrorCode.BusinessException.class,ex->assertThat(ex.errorCode()).isEqualTo(ErrorCode.AWARD_INVALID_TRANSITION));
        assertThatThrownBy(()->AwardDeliveryStateMachine.validate(AwardDeliveryStatus.DISPUTED,AwardDeliveryStatus.DELIVERED)).isInstanceOf(ErrorCode.BusinessException.class);
        assertThatNullPointerException().isThrownBy(()->AwardDeliveryStateMachine.validate(null,AwardDeliveryStatus.VALIDATED));
    }
}
