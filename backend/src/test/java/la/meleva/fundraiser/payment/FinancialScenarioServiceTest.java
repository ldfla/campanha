package la.meleva.fundraiser.payment;

import java.util.List;
import java.util.Optional;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.shared.ErrorCode;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FinancialScenarioServiceTest {
    private final FinancialAccessService access = mock(FinancialAccessService.class);
    private final FinancialScenarioRepository repository = mock(FinancialScenarioRepository.class);
    private final FinancialScenarioService service = new FinancialScenarioService(access, repository);
    private final FinancialSimulation.Scenario input = new FinancialSimulation.Scenario(100, 20, 10, 5,
            500, 200, 100, List.of(new FinancialSimulation.FixedCost(FinancialSimulation.CostCategory.PRIZE, 200)));

    @Test
    void createdScenarioIsClearlyHypotheticalAndUsesBrl() {
        var campaign = mock(Campaign.class);
        when(campaign.getId()).thenReturn(42L);
        when(access.requireAccess(7L, "campaign")).thenReturn(campaign);
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.create(7L, "campaign", "Projeção", input);
        assertThat(result.currency()).isEqualTo("BRL");
        assertThat(result.hypothetical()).isTrue();
        assertThat(result.notice()).contains("SIMULAÇÃO HIPOTÉTICA");
        assertThat(result.scenario()).isEqualTo(input);
    }

    @Test
    void rejectsStaleVersionBeforeChangingTheStoredScenario() {
        var campaign = mock(Campaign.class);
        when(campaign.getId()).thenReturn(42L);
        when(access.requireAccess(7L, "campaign")).thenReturn(campaign);
        var scenario = new FinancialScenario(42L, "Original", input);
        when(repository.findByPublicIdAndCampaignId(scenario.publicId(), 42L)).thenReturn(Optional.of(scenario));

        assertThatThrownBy(() -> service.update(7L, "campaign", scenario.publicId(), 1, "Outro", input))
                .isInstanceOf(ErrorCode.BusinessException.class)
                .satisfies(ex -> assertThat(((ErrorCode.BusinessException) ex).errorCode())
                        .isEqualTo(ErrorCode.FINANCIAL_SCENARIO_VERSION_CONFLICT));
        assertThat(scenario.name()).isEqualTo("Original");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void mapsConcurrentUpdateConflictsToStableError() {
        var campaign = mock(Campaign.class);
        when(campaign.getId()).thenReturn(42L);
        when(access.requireAccess(7L, "campaign")).thenReturn(campaign);
        var scenario = new FinancialScenario(42L, "Original", input);
        when(repository.findByPublicIdAndCampaignId(scenario.publicId(), 42L)).thenReturn(Optional.of(scenario));
        when(repository.saveAndFlush(scenario)).thenThrow(
                new org.springframework.orm.ObjectOptimisticLockingFailureException(FinancialScenario.class, 1L));
        assertThatThrownBy(() -> service.update(7L, "campaign", scenario.publicId(), 0, "Atualizado", input))
                .isInstanceOf(ErrorCode.BusinessException.class)
                .satisfies(ex -> assertThat(((ErrorCode.BusinessException) ex).errorCode())
                        .isEqualTo(ErrorCode.FINANCIAL_SCENARIO_VERSION_CONFLICT));
    }

    @Test
    void rejectsValuesThatOverflowBeforeSaving() {
        var campaign = mock(Campaign.class);
        when(campaign.getId()).thenReturn(42L);
        when(access.requireAccess(7L, "campaign")).thenReturn(campaign);
        var overflow = new FinancialSimulation.Scenario(Long.MAX_VALUE, 0, 2, 2, 0, 0, 0, List.of());
        assertThatThrownBy(() -> service.create(7L, "campaign", "Inválido", overflow))
                .isInstanceOf(ErrorCode.BusinessException.class)
                .satisfies(ex -> assertThat(((ErrorCode.BusinessException) ex).errorCode())
                        .isEqualTo(ErrorCode.VALIDATION_ERROR));
        verifyNoInteractions(repository);
    }

    @Test
    void listsCampaignScenariosInStableIdOrder() {
        var campaign = mock(Campaign.class);
        when(campaign.getId()).thenReturn(42L);
        when(access.requireAccess(7L, "campaign")).thenReturn(campaign);
        when(repository.findAllByCampaignId(eq(42L), any())).thenReturn(org.springframework.data.domain.Page.empty());
        assertThat(service.list(7L, "campaign", 1, 20)).isEmpty();
        var page = org.mockito.ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(repository).findAllByCampaignId(eq(42L), page.capture());
        assertThat(page.getValue().getPageNumber()).isEqualTo(1);
        assertThat(page.getValue().getSort().getOrderFor("id").isAscending()).isTrue();
    }

    @Test
    void deniesAccessBeforeSavingAnyScenario() {
        when(access.requireAccess(7L, "campaign")).thenThrow(ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException());
        assertThatThrownBy(() -> service.create(7L, "campaign", "Novo", input))
                .isInstanceOf(ErrorCode.BusinessException.class);
        verifyNoInteractions(repository);
    }
}
