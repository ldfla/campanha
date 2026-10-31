package la.meleva.fundraiser.award;

import la.meleva.fundraiser.shared.ErrorCode;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AwardTransferPolicyTest {

    @Test void allowsGoodsPrizes() {
        AwardTransferPolicy.requireNonCash("GOODS");
    }

    @Test void refusesCashInAnyCase() {
        for (var type : new String[]{"CASH", "cash", "Cash"}) {
            assertThatThrownBy(() -> AwardTransferPolicy.requireNonCash(type))
                    .isInstanceOfSatisfying(ErrorCode.BusinessException.class,
                            ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.PRIZE_CASH_NOT_ALLOWED));
        }
    }

    @Test void rejectsUnknownPrizeType() {
        assertThatThrownBy(() -> AwardTransferPolicy.requireNonCash(null))
                .isInstanceOfSatisfying(ErrorCode.BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.PRIZE_NOT_FOUND));
    }
}
