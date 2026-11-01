package la.meleva.fundraiser.campaign;

public enum CampaignMode {

    DEMO,
    COMMERCIAL_PROMOTION,
    PHILANTHROPIC_AUTHORIZED,
    /**
     * Título de capitalização premiável (SUSEP) — ADR-0007. Permite cobrança e prêmio em dinheiro
     * **somente** com autorização válida anexada e gates aprovados.
     */
    CAPITALIZATION_PRIZE_TITLE;

    public boolean isDemo() {
        return this == DEMO;
    }

    public boolean allowsRealPayments() {
        return this == PHILANTHROPIC_AUTHORIZED || this == CAPITALIZATION_PRIZE_TITLE;
    }

    public boolean allowsCashPrizes() {
        return this == CAPITALIZATION_PRIZE_TITLE;
    }

    public boolean requiresAuthorization() {
        return this != DEMO;
    }

    public boolean requiresPspContract() {
        return this == PHILANTHROPIC_AUTHORIZED || this == CAPITALIZATION_PRIZE_TITLE;
    }
}
