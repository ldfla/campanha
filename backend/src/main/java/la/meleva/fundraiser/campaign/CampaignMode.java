package la.meleva.fundraiser.campaign;

public enum CampaignMode {

    DEMO,
    COMMERCIAL_PROMOTION,
    PHILANTHROPIC_AUTHORIZED;

    public boolean isDemo() {
        return this == DEMO;
    }

    public boolean allowsRealPayments() {
        return this == PHILANTHROPIC_AUTHORIZED;
    }

    public boolean allowsCashPrizes() {
        return false;
    }

    public boolean requiresAuthorization() {
        return this != DEMO;
    }

    public boolean requiresPspContract() {
        return this == PHILANTHROPIC_AUTHORIZED;
    }
}
