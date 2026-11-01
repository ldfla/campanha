package la.meleva.fundraiser.drawing;

import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Cliente padrão: a integração com a Caixa permanece desligada até existir autorização
 * explícita de acesso e a fórmula aprovada (D-OPEN-008). Use {@code fundraiser.federal.enabled=true}
 * para habilitar o cliente HTTP.
 */
@Component
@ConditionalOnProperty(name = "fundraiser.federal.enabled", havingValue = "false", matchIfMissing = true)
public class DisabledFederalLotteryClient implements FederalLotteryClient {

    @Override
    public FederalContest fetch(int contestNumber) {
        throw ErrorCode.FEDERAL_SOURCE_UNAVAILABLE.toException();
    }
}
