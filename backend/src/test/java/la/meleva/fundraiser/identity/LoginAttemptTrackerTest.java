package la.meleva.fundraiser.identity;

import la.meleva.fundraiser.shared.ErrorCode;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptTrackerTest {

    @Test void locksAfterFiveFailuresAndResetsOnSuccess() {
        var tracker = new LoginAttemptTracker();
        String phone = "+5511999000001";
        assertThatCode(() -> tracker.checkAllowed(phone)).doesNotThrowAnyException();
        for (int attempt = 0; attempt < 5; attempt++) tracker.recordFailure(phone);
        assertThatThrownBy(() -> tracker.checkAllowed(phone))
                .isInstanceOfSatisfying(ErrorCode.BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.AUTH_ACCOUNT_LOCKED));

        String other = "+5511999000002";
        for (int attempt = 0; attempt < 4; attempt++) tracker.recordFailure(other);
        tracker.recordSuccess(other);
        for (int attempt = 0; attempt < 4; attempt++) tracker.recordFailure(other);
        assertThatCode(() -> tracker.checkAllowed(other)).doesNotThrowAnyException();
    }
}
