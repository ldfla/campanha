package la.meleva.fundraiser.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Despesa informada por operador; não movimenta recursos nem comprova pagamento. */
@Entity
@Table(name = "financial_expenses")
class FinancialExpense {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "public_id", nullable = false, length = 36) private String publicId;
    @Column(name = "campaign_id", nullable = false) private Long campaignId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private FinancialSimulation.CostCategory category;
    @Column(name = "amount_cents", nullable = false) private long amountCents;
    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false, length = 200) private String description;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "created_by", nullable = false) private Long createdBy;
    @Column(name = "idempotency_key", nullable = false, length = 36) private String idempotencyKey;

    protected FinancialExpense() { }

    FinancialExpense(Long campaignId, Long actorId, String idempotencyKey,
                     FinancialSimulation.CostCategory category, long amountCents, String description,
                     Instant occurredAt, Instant createdAt) {
        this.campaignId = Objects.requireNonNull(campaignId);
        this.createdBy = Objects.requireNonNull(actorId);
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey);
        this.category = Objects.requireNonNull(category);
        this.description = Objects.requireNonNull(description).strip();
        this.occurredAt = Objects.requireNonNull(occurredAt);
        this.createdAt = Objects.requireNonNull(createdAt);
        if (amountCents <= 0 || this.description.isEmpty() || this.description.length() > 200
                || occurredAt.isAfter(createdAt)) throw new IllegalArgumentException("Despesa inválida");
        this.publicId = UUID.randomUUID().toString();
        this.amountCents = amountCents;
        this.currency = "BRL";
    }

    boolean matches(Long actorId, FinancialSimulation.CostCategory proposedCategory, long proposedCents,
                    String proposedDescription, Instant proposedOccurredAt) {
        return createdBy.equals(actorId) && category == proposedCategory && amountCents == proposedCents
                && description.equals(proposedDescription.strip()) && occurredAt.equals(proposedOccurredAt);
    }
    Long id() { return id; }
    String publicId() { return publicId; }
    FinancialSimulation.CostCategory category() { return category; }
    long amountCents() { return amountCents; }
    String description() { return description; }
    Instant occurredAt() { return occurredAt; }
    Instant createdAt() { return createdAt; }
}
