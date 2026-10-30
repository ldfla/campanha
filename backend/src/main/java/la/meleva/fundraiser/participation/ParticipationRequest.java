package la.meleva.fundraiser.participation;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "participation_requests")
public class ParticipationRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "public_id", nullable = false, updatable = false) private String publicId;
    @Column(name = "campaign_id", nullable = false) private Long campaignId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "idempotency_key", nullable = false) private String idempotencyKey;
    @Column(name = "requested_count", nullable = false) private int requestedCount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private ParticipationStatus status;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected ParticipationRequest() {}
    public ParticipationRequest(Long campaignId, Long userId, String idempotencyKey, int requestedCount) {
        if (requestedCount < 1) throw new IllegalArgumentException("Quantidade solicitada deve ser positiva");
        this.publicId = UUID.randomUUID().toString(); this.campaignId = campaignId; this.userId = userId;
        this.idempotencyKey = idempotencyKey; this.requestedCount = requestedCount; this.status = ParticipationStatus.REQUESTED;
        this.createdAt = Instant.now(); this.updatedAt = createdAt;
    }
    public Long getId() { return id; }
    public Long getCampaignId() { return campaignId; }
    public Long getUserId() { return userId; }
    public String getIdempotencyKey() { return idempotencyKey; }
}
