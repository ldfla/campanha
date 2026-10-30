package la.meleva.fundraiser.participation;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "participation_entries")
public class ParticipationEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "public_id", nullable = false, updatable = false) private String publicId;
    @Column(name = "campaign_id", nullable = false) private Long campaignId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "request_id", nullable = false) private Long requestId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private ParticipationStatus status;
    @Column(name = "eligibility_snapshot_hash", length = 64) private String eligibilitySnapshotHash;
    @Column(name = "issued_at") private Instant issuedAt;
    @Column(name = "validated_at") private Instant validatedAt;
    @Column(nullable = false, length = 50) private String source;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected ParticipationEntry() {}
    public ParticipationEntry(Long campaignId, Long userId, Long requestId, String source) {
        this.publicId = UUID.randomUUID().toString(); this.campaignId = campaignId; this.userId = userId; this.requestId = requestId;
        this.status = ParticipationStatus.REQUESTED; this.source = source; this.createdAt = Instant.now(); this.updatedAt = createdAt;
    }
    public Long getId() { return id; }
    public Long getCampaignId() { return campaignId; }
    public Long getUserId() { return userId; }
}
