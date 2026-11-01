package la.meleva.fundraiser.drawing;

/** Fonte oficial do resultado da Loteria Federal (P12). */
public interface FederalLotteryClient {

    /**
     * Busca um concurso pelo número.
     *
     * @throws la.meleva.fundraiser.shared.ErrorCode.BusinessException com
     *         {@code FEDERAL_SOURCE_UNAVAILABLE} quando a fonte não está disponível.
     */
    FederalContest fetch(int contestNumber);
}
