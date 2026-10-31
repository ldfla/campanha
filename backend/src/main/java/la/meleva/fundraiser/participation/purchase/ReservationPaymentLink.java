package la.meleva.fundraiser.participation.purchase;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "reservation_payment_links")
public class ReservationPaymentLink {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private @Nullable Long id;
    @Column(name = "request_id", nullable = false, unique = true, updatable = false) private Long requestId;
    @Column(name = "order_id", nullable = false, unique = true, updatable = false) private Long orderId;
    @Column(name = "offer_id", nullable = false, updatable = false) private Long offerId;
    @Column(name = "total_cents", nullable = false, updatable = false) private long totalCents;
    @Column(name = "referral_code", length = 20, updatable = false) private @Nullable String referralCode;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected ReservationPaymentLink() { }
    public ReservationPaymentLink(Long requestId, Long orderId, Long offerId, long totalCents, @Nullable String referralCode) {
        this.requestId = Objects.requireNonNull(requestId, "requestId");
        this.orderId = Objects.requireNonNull(orderId, "orderId");
        this.offerId = Objects.requireNonNull(offerId, "offerId");
        if (totalCents < 1) throw new IllegalArgumentException("reservation_payment.invalid_total");
        this.totalCents = totalCents; this.referralCode = referralCode; this.createdAt = Instant.now();
    }
    public Long getOrderId() { return orderId; }
    public Long getOfferId() { return offerId; }
    public long getTotalCents() { return totalCents; }
    public Optional<String> referralCode() { return Optional.ofNullable(referralCode); }
}
