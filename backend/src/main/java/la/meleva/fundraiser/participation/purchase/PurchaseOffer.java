package la.meleva.fundraiser.participation.purchase;

import jakarta.persistence.*;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "campaign_purchase_offers")
public class PurchaseOffer {
    public static final int MIN_QUANTITY = 5;
    public static final int MAX_QUANTITY = 5_000;
    public static final long MIN_TOTAL_CENTS = 500;

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
        if (!validTerms(quantity, totalCents)) throw new IllegalArgumentException("purchase_offer.invalid");
        this.quantity = quantity; this.totalCents = totalCents; this.currency = "BRL";
        this.publicId = UUID.randomUUID().toString(); this.active = true;
    }
    public static boolean validTerms(int quantity, long totalCents) {
        return quantity >= MIN_QUANTITY && quantity <= MAX_QUANTITY && totalCents >= MIN_TOTAL_CENTS;
    }
    public Long getId() { return Objects.requireNonNull(id, "purchase_offer.id"); }
    public String getPublicId() { return publicId; }
    public int getQuantity() { return quantity; }
    public long getTotalCents() { return totalCents; }
}
