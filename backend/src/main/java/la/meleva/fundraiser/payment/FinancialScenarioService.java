package la.meleva.fundraiser.payment;

import java.util.List;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class FinancialScenarioService {
    private final FinancialAccessService access;
    private final FinancialScenarioRepository scenarios;

    FinancialScenarioService(FinancialAccessService access, FinancialScenarioRepository scenarios) {
        this.access = access;
        this.scenarios = scenarios;
    }

    @Transactional
    View create(Long actorId, String campaignId, String name, FinancialSimulation.Scenario input) {
        var campaign = access.requireAccess(actorId, campaignId);
        validate(input);
        return view(scenarios.saveAndFlush(new FinancialScenario(campaign.getId(), name, input)));
    }

    @Transactional
    View update(Long actorId, String campaignId, String scenarioId, long version, String name,
                FinancialSimulation.Scenario input) {
        var campaign = access.requireAccess(actorId, campaignId);
        var scenario = scenarios.findByPublicIdAndCampaignId(scenarioId, campaign.getId())
                .orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        if (scenario.version() != version) throw ErrorCode.FINANCIAL_SCENARIO_VERSION_CONFLICT.toException();
        validate(input);
        scenario.update(name, input);
        try { return view(scenarios.saveAndFlush(scenario)); }
        catch (org.springframework.dao.OptimisticLockingFailureException ex) {
            throw ErrorCode.FINANCIAL_SCENARIO_VERSION_CONFLICT.toException();
        }
    }

    @Transactional(readOnly = true)
    List<View> list(Long actorId, String campaignId, int page, int size) {
        var campaign = access.requireAccess(actorId, campaignId);
        return scenarios.findAllByCampaignId(campaign.getId(), PageRequest.of(page, size, Sort.by("id").ascending())).map(this::view).getContent();
    }

    private void validate(FinancialSimulation.Scenario input) {
        try { FinancialSimulation.calculate(input); }
        catch (IllegalArgumentException | ArithmeticException ex) { throw ErrorCode.VALIDATION_ERROR.toException(); }
    }

    private View view(FinancialScenario entity) {
        return new View(entity.publicId(), entity.name(), entity.version(), entity.scenario(), "BRL", true,
                "SIMULAÇÃO HIPOTÉTICA: não representa caixa real nem autorização de operação.");
    }

    record View(String id, String name, long version, FinancialSimulation.Scenario scenario,
                String currency, boolean hypothetical, String notice) { }
}
