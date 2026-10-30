package la.meleva.fundraiser.campaign;

public enum CampaignStatus {

    DRAFT,
    LEGAL_REVIEW,
    AUTHORIZED,
    SCHEDULED,
    OPEN,
    LOCKED,
    DRAW_PENDING,
    DRAWN,
    AWARDS_PENDING,
    CLOSED,
    SUSPENDED,
    CANCELLED;

    public boolean isEditable() {
        return this == DRAFT;
    }

    public boolean isActive() {
        return this == OPEN || this == LOCKED || this == DRAW_PENDING;
    }

    public boolean isFinal() {
        return this == CLOSED || this == CANCELLED;
    }

    public boolean acceptsParticipations() {
        return this == OPEN;
    }
}
