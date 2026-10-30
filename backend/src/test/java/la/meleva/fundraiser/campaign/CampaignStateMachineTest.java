package la.meleva.fundraiser.campaign;

import la.meleva.fundraiser.shared.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CampaignStateMachineTest {

    @Test
    void draftCanTransitionToLegalReview() {
        assertDoesNotThrow(() ->
                CampaignStateMachine.validate(CampaignStatus.DRAFT, CampaignStatus.LEGAL_REVIEW));
    }

    @Test
    void draftCanBeCancelled() {
        assertDoesNotThrow(() ->
                CampaignStateMachine.validate(CampaignStatus.DRAFT, CampaignStatus.CANCELLED));
    }

    @Test
    void draftCannotTransitionToOpen() {
        var ex = assertThrows(ErrorCode.BusinessException.class, () ->
                CampaignStateMachine.validate(CampaignStatus.DRAFT, CampaignStatus.OPEN));
        assertEquals(ErrorCode.CAMPAIGN_INVALID_TRANSITION, ex.errorCode());
    }

    @Test
    void legalReviewCanGoBackToDraft() {
        assertDoesNotThrow(() ->
                CampaignStateMachine.validate(CampaignStatus.LEGAL_REVIEW, CampaignStatus.DRAFT));
    }

    @Test
    void authorizedCanTransitionToScheduled() {
        assertDoesNotThrow(() ->
                CampaignStateMachine.validate(CampaignStatus.AUTHORIZED, CampaignStatus.SCHEDULED));
    }

    @Test
    void openCanBeLocked() {
        assertDoesNotThrow(() ->
                CampaignStateMachine.validate(CampaignStatus.OPEN, CampaignStatus.LOCKED));
    }

    @Test
    void openCanBeSuspended() {
        assertDoesNotThrow(() ->
                CampaignStateMachine.validate(CampaignStatus.OPEN, CampaignStatus.SUSPENDED));
    }

    @Test
    void suspendedCanReopen() {
        assertDoesNotThrow(() ->
                CampaignStateMachine.validate(CampaignStatus.SUSPENDED, CampaignStatus.OPEN));
    }

    @Test
    void closedIsTerminal() {
        Set<CampaignStatus> allowed = CampaignStateMachine.allowedTransitions(CampaignStatus.CLOSED);
        assertTrue(allowed.isEmpty());
    }

    @Test
    void cancelledIsTerminal() {
        Set<CampaignStatus> allowed = CampaignStateMachine.allowedTransitions(CampaignStatus.CANCELLED);
        assertTrue(allowed.isEmpty());
    }

    @Test
    void fullHappyPath() {
        assertDoesNotThrow(() -> {
            CampaignStateMachine.validate(CampaignStatus.DRAFT, CampaignStatus.LEGAL_REVIEW);
            CampaignStateMachine.validate(CampaignStatus.LEGAL_REVIEW, CampaignStatus.AUTHORIZED);
            CampaignStateMachine.validate(CampaignStatus.AUTHORIZED, CampaignStatus.SCHEDULED);
            CampaignStateMachine.validate(CampaignStatus.SCHEDULED, CampaignStatus.OPEN);
            CampaignStateMachine.validate(CampaignStatus.OPEN, CampaignStatus.LOCKED);
            CampaignStateMachine.validate(CampaignStatus.LOCKED, CampaignStatus.DRAW_PENDING);
            CampaignStateMachine.validate(CampaignStatus.DRAW_PENDING, CampaignStatus.DRAWN);
            CampaignStateMachine.validate(CampaignStatus.DRAWN, CampaignStatus.AWARDS_PENDING);
            CampaignStateMachine.validate(CampaignStatus.AWARDS_PENDING, CampaignStatus.CLOSED);
        });
    }
}
