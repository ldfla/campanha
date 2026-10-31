package la.meleva.fundraiser.compliance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import la.meleva.fundraiser.shared.ErrorCode;

@Entity
@Table(name = "compliance_documents")
public class ComplianceDocument {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "organization_id") private Long organizationId;
    @Column(name = "campaign_id") private Long campaignId;
    @Column(nullable = false) private String kind;
    @Column(name = "object_key", nullable = false) private String objectKey;
    @Column(nullable = false, length = 64) private String sha256;
    @Column(nullable = false) private String classification;
    @Column(nullable = false) private String status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected ComplianceDocument() {}

    public ComplianceDocument(Long organizationId, Long campaignId, String kind, String objectKey,
                              String sha256, DocumentClassification classification) {
        this.organizationId = organizationId;
        this.campaignId = campaignId;
        this.kind = kind;
        this.objectKey = objectKey;
        this.sha256 = sha256;
        this.classification = classification.name();
        this.status = "ACTIVE";
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getCampaignId() { return campaignId; }
    public String getKind() { return kind; }
    public String getSha256() { return sha256; }
    public DocumentClassification getClassification() { return DocumentClassification.valueOf(classification); }
    public String getObjectKey() { return objectKey; }
    public boolean isPublishable() { return getClassification() == DocumentClassification.PUBLISHABLE; }

    public void requirePrivate() {
        if (isPublishable()) throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
    }
}
