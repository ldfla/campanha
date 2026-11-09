package la.meleva.fundraiser.payment;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import la.meleva.fundraiser.audit.AuditEventRepository;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.identity.Membership;
import la.meleva.fundraiser.identity.MembershipRepository;
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.shared.ErrorCode;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FinancialExpenseServiceTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final String KEY = "2512abae-f1e9-448b-a06b-6bc859694a5b";
    private final CampaignRepository campaigns = mock(CampaignRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final MembershipRepository memberships = mock(MembershipRepository.class);
    private final FinancialExpenseRepository expenses = mock(FinancialExpenseRepository.class);
    private final AuditEventRepository audits = mock(AuditEventRepository.class);
    private final FinancialExpenseService service = new FinancialExpenseService(campaigns, users, memberships,
            expenses, audits, Clock.fixed(NOW, ZoneOffset.UTC));

    private void authorize() {
        var campaign = mock(Campaign.class);
        var user = mock(User.class);
        when(campaign.getId()).thenReturn(1L);
        when(campaign.getOrganizationId()).thenReturn(10L);
        when(campaigns.findLockedByPublicId("campaign")).thenReturn(Optional.of(campaign));
        when(user.isActive()).thenReturn(true);
        when(user.getId()).thenReturn(2L);
        when(users.lockForAuthorization(2L)).thenReturn(Optional.of(user));
        when(memberships.lockActiveForAuthorization(eq(2L), eq(10L), anyList(), any(), any()))
                .thenReturn(List.of(mock(Membership.class)));
    }

    @Test
    void replaysMatchingExpenseAndRejectsConflictingAmount() {
        authorize();
        var existing = new FinancialExpense(1L, 2L, KEY, FinancialSimulation.CostCategory.PLATFORM,
                100, "Tarifa", NOW, NOW);
        when(expenses.findByCampaignIdAndIdempotencyKey(1L, KEY)).thenReturn(Optional.of(existing));
        assertThat(service.create(2L, "campaign", KEY, FinancialSimulation.CostCategory.PLATFORM,
                100, "Tarifa", NOW).id()).isEqualTo(existing.publicId());
        assertThatThrownBy(() -> service.create(2L, "campaign", KEY, FinancialSimulation.CostCategory.PLATFORM,
                101, "Tarifa", NOW)).isInstanceOf(ErrorCode.BusinessException.class)
                .satisfies(ex -> assertThat(((ErrorCode.BusinessException) ex).errorCode())
                        .isEqualTo(ErrorCode.IDEMPOTENCY_CONFLICT));
        verify(expenses, never()).saveAndFlush(any());
        verifyNoInteractions(audits);
    }

    @Test
    void refusesMissingFinanceMembershipBeforePersisting() {
        authorize();
        when(memberships.lockActiveForAuthorization(eq(2L), eq(10L), anyList(), any(), any())).thenReturn(List.of());
        assertThatThrownBy(() -> service.create(2L, "campaign", KEY, FinancialSimulation.CostCategory.PLATFORM,
                100, "Tarifa", NOW)).isInstanceOf(ErrorCode.BusinessException.class)
                .satisfies(ex -> assertThat(((ErrorCode.BusinessException) ex).errorCode())
                        .isEqualTo(ErrorCode.AUTH_RESOURCE_ACCESS_DENIED));
        verifyNoInteractions(expenses, audits);
    }

    @Test
    void rejectsFutureExpenseAndNoncanonicalIdempotencyKey() {
        authorize();
        assertThatThrownBy(() -> service.create(2L, "campaign", KEY, FinancialSimulation.CostCategory.PLATFORM,
                100, "Tarifa", NOW.plusSeconds(1))).isInstanceOf(ErrorCode.BusinessException.class);
        assertThatThrownBy(() -> service.create(2L, "campaign", "1-1-1-1-1", FinancialSimulation.CostCategory.PLATFORM,
                100, "Tarifa", NOW)).isInstanceOf(ErrorCode.BusinessException.class);
        verifyNoInteractions(expenses, audits);
    }

    @Test
    void rejectsAmountsOutsideExactJsonIntegerRangeAndDatesOutsideMysqlRange() {
        authorize();
        assertThatThrownBy(() -> service.create(2L, "campaign", KEY, FinancialSimulation.CostCategory.PLATFORM,
                FinancialExpenseService.MAX_AMOUNT_CENTS + 1, "Tarifa", NOW))
                .isInstanceOf(ErrorCode.BusinessException.class);
        assertThatThrownBy(() -> service.create(2L, "campaign", KEY, FinancialSimulation.CostCategory.PLATFORM,
                100, "Tarifa", Instant.parse("1969-12-31T23:59:59Z")))
                .isInstanceOf(ErrorCode.BusinessException.class);
        verifyNoInteractions(expenses, audits);
    }
}
