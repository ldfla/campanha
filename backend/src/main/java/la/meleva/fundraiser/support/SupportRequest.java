package la.meleva.fundraiser.support;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import la.meleva.fundraiser.shared.ErrorCode;

/** Solicitação de suporte. Não armazena nome, telefone ou e-mail do autor — apenas o vínculo interno. */
@Entity
@Table(name = "support_requests")
public class SupportRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "public_id", nullable = false, updatable = false, length = 36) private String publicId;
    @Column(name = "user_id", nullable = false, updatable = false) private Long userId;
    @Column(name = "organization_id") private Long organizationId;
    @Column(name = "idempotency_key", length = 36, updatable = false) private String idempotencyKey;
    @Column(name = "idempotency_fingerprint", length = 64, updatable = false) private String idempotencyFingerprint;
    @Column(nullable = false, length = 150) private String subject;
    @Column(nullable = false, length = 2000) private String message;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private SupportStatus status;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "status_revision", nullable = false) private long statusRevision;
    @Column(name = "has_reopened", nullable = false) private boolean hasReopened;

    protected SupportRequest() { }

    public SupportRequest(Long userId, String subject, String message) {
        this.userId = Objects.requireNonNull(userId, "userId");
        this.subject = Objects.requireNonNull(subject, "subject");
        this.message = Objects.requireNonNull(message, "message");
        this.publicId = UUID.randomUUID().toString();
        this.status = SupportStatus.OPEN;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public Long getId() { return id; }
    public String getPublicId() { return publicId; }
    public Long getUserId() { return userId; }
    public Long getOrganizationId() { return organizationId; }
    String getIdempotencyFingerprint() { return idempotencyFingerprint; }
    void setIdempotency(String key, String fingerprint) {
        this.idempotencyKey = Objects.requireNonNull(key);
        this.idempotencyFingerprint = Objects.requireNonNull(fingerprint);
    }
    void assignOrganization(Long organizationId) { this.organizationId = Objects.requireNonNull(organizationId); }
    public String getSubject() { return subject; }
    public String getMessage() { return message; }
    public SupportStatus getStatus() { return status; }
    public long getStatusRevision() { return statusRevision; }

    boolean transitionTo(SupportStatus next, Instant now, Long expectedRevision) {
        Objects.requireNonNull(next, "next");
        Objects.requireNonNull(now, "now");
        boolean reopening = status == SupportStatus.CLOSED && next == SupportStatus.IN_PROGRESS;
        if ((expectedRevision != null && expectedRevision != statusRevision)
                || (expectedRevision == null && (hasReopened || reopening)))
            throw ErrorCode.SUPPORT_STATUS_TRANSITION_CONFLICT.toException();
        if (status == next) return false;
        if (!((status == SupportStatus.OPEN && next == SupportStatus.IN_PROGRESS)
                || (status == SupportStatus.IN_PROGRESS && next == SupportStatus.CLOSED)
                || (status == SupportStatus.CLOSED && next == SupportStatus.IN_PROGRESS)))
            throw ErrorCode.SUPPORT_STATUS_TRANSITION_CONFLICT.toException();
        status = next;
        statusRevision = Math.addExact(statusRevision, 1);
        if (reopening) hasReopened = true;
        updatedAt = now;
        return true;
    }

    public enum SupportStatus { OPEN, IN_PROGRESS, CLOSED }
}
