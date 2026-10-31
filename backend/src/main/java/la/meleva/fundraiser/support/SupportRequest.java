package la.meleva.fundraiser.support;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Solicitação de suporte. Não armazena nome, telefone ou e-mail do autor — apenas o vínculo interno. */
@Entity
@Table(name = "support_requests")
public class SupportRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "public_id", nullable = false, updatable = false, length = 36) private String publicId;
    @Column(name = "user_id", nullable = false, updatable = false) private Long userId;
    @Column(nullable = false, length = 150) private String subject;
    @Column(nullable = false, length = 2000) private String message;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private SupportStatus status;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

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
    public String getSubject() { return subject; }
    public String getMessage() { return message; }
    public SupportStatus getStatus() { return status; }

    public enum SupportStatus { OPEN, IN_PROGRESS, CLOSED }
}
