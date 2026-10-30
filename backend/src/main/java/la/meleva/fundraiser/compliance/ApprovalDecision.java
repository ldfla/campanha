package la.meleva.fundraiser.compliance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "approval_decisions")
public class ApprovalDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "reviewer_user_id", nullable = false)
    private Long reviewerUserId;

    @Column(nullable = false, length = 30)
    private String decision;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "decided_at", nullable = false, updatable = false)
    private Instant decidedAt;

    protected ApprovalDecision() {}

    public ApprovalDecision(Long requestId, Long reviewerUserId, String decision, String reason) {
        this.requestId = requestId;
        this.reviewerUserId = reviewerUserId;
        this.decision = decision;
        this.reason = reason;
        this.decidedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getRequestId() { return requestId; }
    public Long getReviewerUserId() { return reviewerUserId; }
    public String getDecision() { return decision; }
    public String getReason() { return reason; }
    public Instant getDecidedAt() { return decidedAt; }
}
