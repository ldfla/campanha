package la.meleva.fundraiser.payment;

import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinancialSimulationService {
    private final FinancialAccessService access;

    FinancialSimulationService(FinancialAccessService access) {
        this.access = access;
    }

    @Transactional(readOnly = true)
    public FinancialSimulation.Result calculate(Long actorId, String campaignId,
                                                 FinancialSimulation.Scenario scenario) {
        access.requireAccess(actorId, campaignId);
        try {
            return FinancialSimulation.calculate(scenario);
        } catch (IllegalArgumentException | ArithmeticException exception) {
            throw ErrorCode.VALIDATION_ERROR.toException();
        }
    }
}
