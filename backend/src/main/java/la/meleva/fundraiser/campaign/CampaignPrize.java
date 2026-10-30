package la.meleva.fundraiser.campaign;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "campaign_prizes")
public class CampaignPrize {

    public enum PrizeType {
        GOODS, CASH
    }

    public enum PrizeStatus {
        ACTIVE, INACTIVE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PrizeType type;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "value_cents", nullable = false)
    private long valueCents;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PrizeStatus status;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CampaignPrize() {}

    public CampaignPrize(Long campaignId, PrizeType type, String description,
                         long valueCents, int quantity, int sortOrder) {
        this.campaignId = campaignId;
        this.type = type;
        this.description = description;
        this.valueCents = valueCents;
        this.currency = "BRL";
        this.quantity = quantity;
        this.status = PrizeStatus.ACTIVE;
        this.sortOrder = sortOrder;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getCampaignId() { return campaignId; }
    public PrizeType getType() { return type; }
    public String getDescription() { return description; }
    public long getValueCents() { return valueCents; }
    public String getCurrency() { return currency; }
    public int getQuantity() { return quantity; }
    public PrizeStatus getStatus() { return status; }
    public int getSortOrder() { return sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(String description, long valueCents, int quantity, int sortOrder) {
        this.description = description;
        this.valueCents = valueCents;
        this.quantity = quantity;
        this.sortOrder = sortOrder;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.status = PrizeStatus.INACTIVE;
        this.updatedAt = Instant.now();
    }
}
