package la.meleva.fundraiser.shared;

public enum ErrorCode {

    // Campanha
    CAMPAIGN_NOT_FOUND("Campanha não encontrada", 404),
    CAMPAIGN_NOT_EDITABLE("Campanha não pode ser editada neste estado", 409),
    CAMPAIGN_OPERATION_BLOCKED("Operação não permitida para o modo atual da campanha", 403),
    CAMPAIGN_NOT_AUTHORIZED("Campanha não autorizada para operação real", 403),
    CAMPAIGN_INVALID_TRANSITION("Transição de estado inválida", 409),
    CAMPAIGN_GATES_INCOMPLETE("Gates de aprovação incompletos para esta operação", 403),
    CAMPAIGN_LIVE_OPERATIONS_DISABLED("Operações reais desativadas globalmente", 403),

    // Participação
    PARTICIPATION_DUPLICATE("Participação duplicada para esta chave de idempotência", 409),
    PARTICIPATION_NOT_ELIGIBLE("Participante não elegível para esta campanha", 403),
    PARTICIPATION_CAMPAIGN_CLOSED("Campanha não aceita participações neste momento", 409),
    PARTICIPATION_LIMIT_EXCEEDED("Limite de participações por usuário atingido", 409),

    // Números
    NUMBER_NOT_AVAILABLE("Número não disponível para atribuição", 409),
    NUMBER_ALREADY_ASSIGNED("Número já atribuído a outro participante", 409),
    NUMBER_POOL_EXHAUSTED("Todos os números da campanha foram atribuídos", 409),
    NUMBER_FROZEN("Números congelados para apuração", 409),

    // Pagamento
    PAYMENT_NOT_ALLOWED("Cobrança não permitida para este modo de campanha", 403),
    PAYMENT_ORDER_EXPIRED("Pedido expirado", 410),
    PAYMENT_DUPLICATE_EVENT("Evento de pagamento duplicado", 409),
    PAYMENT_PROVIDER_ERROR("Erro no provedor de pagamento", 502),
    PAYMENT_NOT_RECONCILED("Pagamento ainda não conciliado", 409),
    PAYMENT_WEBHOOK_INVALID("Assinatura de webhook inválida", 401),

    // Prêmio
    PRIZE_CASH_NOT_ALLOWED("Prêmio em dinheiro não permitido para promoção comercial", 403),
    PRIZE_NOT_FOUND("Prêmio não encontrado", 404),
    AWARD_INVALID_TRANSITION("Transição de entrega de prêmio inválida", 409),

    // Indicação
    REFERRAL_SELF_REFERRAL("Autoindicação não permitida", 409),
    REFERRAL_PROMOTER_NOT_ELIGIBLE("Divulgador precisa ter ao menos uma cota comprada para indicar", 409),
    REFERRAL_ALREADY_ATTRIBUTED("Participação já atribuída a outra indicação", 409),

    // Ranking
    RANKING_RULE_INVALID("Regra de ranking inválida para o escopo informado", 400),
    RANKING_RULE_NOT_FOUND("Nenhuma regra de ranking vigente para a campanha", 404),
    RANKING_SNAPSHOT_NOT_FOUND("Snapshot de ranking não encontrado", 404),
    RANKING_SNAPSHOT_HASH_MISMATCH("Hash do snapshot de ranking não confere", 409),

    // Sorteio
    DRAW_RESULTS_NOT_VERIFIED("Resultados oficiais não verificados por dois revisores", 409),
    DRAW_RULE_MISMATCH("Regra de apuração não corresponde ao regulamento aprovado", 409),
    DRAW_ALREADY_PUBLISHED("Resultado já publicado e não pode ser alterado", 409),
    DRAW_SURPRISE_COMMITMENT_FAILED("Hash de compromisso dos números-surpresa não confere", 409),
    DRAW_STATE_INVALID("Operação inválida para o estado atual da apuração", 409),
    FEDERAL_SOURCE_UNAVAILABLE("Fonte oficial da apuração indisponível", 502),

    PRIZE_LOAD_SEALED("Lista de números premiados lacrada até o encerramento das vendas", 409),

    // Aprovação
    APPROVAL_SELF_APPROVAL("Autoaprovação não permitida", 403),
    APPROVAL_ALREADY_DECIDED("Aprovação já decidida", 409),

    // Identidade
    AUTH_INVALID_CREDENTIALS("Credenciais inválidas", 401),
    AUTH_DUPLICATE_CREDENTIAL("Número de telefone já cadastrado", 409),
    AUTH_ACCOUNT_LOCKED("Conta bloqueada por excesso de tentativas", 429),
    AUTH_SESSION_EXPIRED("Sessão expirada", 401),
    AUTH_MFA_REQUIRED("Autenticação multifator obrigatória", 403),
    AUTH_INSUFFICIENT_ROLE("Papel insuficiente para esta operação", 403),
    AUTH_RESOURCE_ACCESS_DENIED("Acesso negado a este recurso", 403),

    // Privacidade
    PRIVACY_REQUEST_DUPLICATE("Solicitação de privacidade duplicada", 409),

    // Geral
    VALIDATION_ERROR("Erro de validação nos dados enviados", 400),
    RESOURCE_NOT_FOUND("Recurso não encontrado", 404),
    CPF_INVALID("CPF inválido", 400),
    CPF_ALREADY_REGISTERED("CPF já cadastrado", 409),
    CPF_NOT_CONFIGURED("Armazenamento seguro de CPF indisponível", 503),
    IDEMPOTENCY_CONFLICT("Requisição com mesma chave de idempotência já processada", 409),
    INTERNAL_ERROR("Erro interno do servidor", 500);

    private final String message;
    private final int httpStatus;

    ErrorCode(String message, int httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String title() {
        return message;
    }

    public String message() {
        return message;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String code() {
        return name();
    }

    public BusinessException toException() {
        return new BusinessException(this);
    }

    public static class BusinessException extends RuntimeException {
        private final ErrorCode errorCode;

        public BusinessException(ErrorCode errorCode) {
            super(errorCode.message());
            this.errorCode = errorCode;
        }

        public ErrorCode errorCode() {
            return errorCode;
        }
    }
}
