package la.meleva.fundraiser.participation;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "reservation_leases")
public class ReservationLease {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "campaign_id", nullable = false) private Long campaignId;
    @Column(name = "number_id", nullable = false) private Long numberId;
    @Column(name = "request_id", nullable = false) private Long requestId;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(nullable = false, length = 30) private String status;
    @Version private int version;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected ReservationLease() { }

    public ReservationLease(Long campaignId, Long numberId, Long requestId, Instant expiresAt) {
        this.campaignId = Objects.requireNonNull(campaignId, "campaignId");
        this.numberId = Objects.requireNonNull(numberId, "numberId");
        this.requestId = Objects.requireNonNull(requestId, "requestId");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.status = "ACTIVE";
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getNumberId() { return numberId; }
    public Long getRequestId() { return requestId; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isExpiredAt(Instant now) {
        return "ACTIVE".equals(status) && !Objects.requireNonNull(now, "now").isBefore(expiresAt);
    }
    public void expire(Instant now) {
        if (!isExpiredAt(now)) throw new IllegalStateException("reservation.not_expired");
        status = "EXPIRED";
    }
    public void consume(Instant now) {
        Objects.requireNonNull(now, "now");
        if (!"ACTIVE".equals(status) || !now.isBefore(expiresAt)) throw new IllegalStateException("reservation.not_active");
        status = "CONSUMED";
    }
}
