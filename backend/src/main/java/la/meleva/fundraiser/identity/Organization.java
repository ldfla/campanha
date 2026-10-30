package la.meleva.fundraiser.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organizations")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false, length = 36)
    private String publicId;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrgStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    protected Organization() {}

    public Organization(String legalName, String type) {
        this.publicId = UUID.randomUUID().toString();
        this.legalName = legalName;
        this.type = type;
        this.status = OrgStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() { return id; }
    public String getPublicId() { return publicId; }
    public String getLegalName() { return legalName; }
    public String getType() { return type; }
    public OrgStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public enum OrgStatus {
        ACTIVE, SUSPENDED, DEACTIVATED
    }
}
