package la.meleva.fundraiser.compliance;

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
@Table(name = "approval_requests")
public class ApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Enumerated(EnumType.STRING)
    @Column(name = "gate_type", nullable = false, length = 30)
    private GateType gateType;

    @Column(name = "requested_by", nullable = false)
    private Long requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApprovalStatus status;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected ApprovalRequest() {}

    public ApprovalRequest(Long campaignId, GateType gateType, Long requestedBy) {
        this.campaignId = campaignId;
        this.gateType = gateType;
        this.requestedBy = requestedBy;
        this.status = ApprovalStatus.PENDING;
        this.requestedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getCampaignId() { return campaignId; }
    public GateType getGateType() { return gateType; }
    public Long getRequestedBy() { return requestedBy; }
    public ApprovalStatus getStatus() { return status; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getResolvedAt() { return resolvedAt; }

    public void approve() {
        requirePending();
        this.status = ApprovalStatus.APPROVED;
        this.resolvedAt = Instant.now();
    }

    public void reject() {
        requirePending();
        this.status = ApprovalStatus.REJECTED;
        this.resolvedAt = Instant.now();
    }

    public void revoke() {
        if (this.status != ApprovalStatus.APPROVED) {
            throw new IllegalStateException("Apenas aprovações podem ser revogadas");
        }
        this.status = ApprovalStatus.REVOKED;
        this.resolvedAt = Instant.now();
    }

    private void requirePending() {
        if (this.status != ApprovalStatus.PENDING) {
            throw la.meleva.fundraiser.shared.ErrorCode.APPROVAL_ALREADY_DECIDED.toException();
        }
    }
}
