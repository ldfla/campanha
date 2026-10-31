package la.meleva.fundraiser.participation.purchase;

import jakarta.persistence.*;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "campaign_purchase_offers")
public class PurchaseOffer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private @Nullable Long id;
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 36) private String publicId;
    @Column(name = "campaign_id", nullable = false, updatable = false) private Long campaignId;
    @Column(nullable = false) private int quantity;
    @Column(name = "total_cents", nullable = false) private long totalCents;
    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false) private boolean active;

    protected PurchaseOffer() { }
    public PurchaseOffer(Long campaignId, int quantity, long totalCents) {
        this.campaignId = Objects.requireNonNull(campaignId, "campaignId");
        if (quantity < 1 || quantity > 100 || totalCents < 1) throw new IllegalArgumentException("purchase_offer.invalid");
        this.quantity = quantity; this.totalCents = totalCents; this.currency = "BRL";
        this.publicId = UUID.randomUUID().toString(); this.active = true;
    }
    public Long getId() { return Objects.requireNonNull(id, "purchase_offer.id"); }
    public String getPublicId() { return publicId; }
    public int getQuantity() { return quantity; }
    public long getTotalCents() { return totalCents; }
}
