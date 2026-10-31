package la.meleva.fundraiser.award;

import la.meleva.fundraiser.shared.ErrorCode;

/**
 * Premiação em dinheiro não é permitida em nenhuma modalidade desta plataforma. A verificação é
 * redundante com o filtro da apuração DEMO, mas falha com código específico em vez de "não elegível".
 */
public final class AwardTransferPolicy {

    private AwardTransferPolicy() {}

    public static void requireNonCash(String prizeType) {
        if (prizeType == null) throw ErrorCode.PRIZE_NOT_FOUND.toException();
        if ("CASH".equalsIgnoreCase(prizeType)) throw ErrorCode.PRIZE_CASH_NOT_ALLOWED.toException();
    }
}
