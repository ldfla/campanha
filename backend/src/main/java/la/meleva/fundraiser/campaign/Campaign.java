package la.meleva.fundraiser.campaign;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import la.meleva.fundraiser.shared.ErrorCode;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "campaigns")
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private String publicId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, unique = true)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CampaignMode mode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CampaignStatus status;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(nullable = false, length = 50)
    private String timezone;

    @Column(name = "series_count", nullable = false)
    private int seriesCount;

    @Column(name = "number_min", nullable = false)
    private int numberMin;

    @Column(name = "number_max", nullable = false)
    private int numberMax;

    @Column(name = "current_regulation_id")
    private Long currentRegulationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private int version;

    protected Campaign() {}

    public Campaign(Long organizationId, String title, String slug, CampaignMode mode) {
        this.publicId = UUID.randomUUID().toString();
        this.organizationId = organizationId;
        this.title = title;
        this.slug = slug;
        this.mode = mode;
        this.status = CampaignStatus.DRAFT;
        this.timezone = "America/Sao_Paulo";
        this.seriesCount = 1;
        this.numberMin = 0;
        this.numberMax = 99999;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getPublicId() { return publicId; }
    public Long getOrganizationId() { return organizationId; }
    public String getSlug() { return slug; }
    public CampaignMode getMode() { return mode; }
    public CampaignStatus getStatus() { return status; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public String getTimezone() { return timezone; }
    public int getSeriesCount() { return seriesCount; }
    public int getNumberMin() { return numberMin; }
    public int getNumberMax() { return numberMax; }
    public Long getCurrentRegulationId() { return currentRegulationId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public int getVersion() { return version; }

    public void setTitle(String title) {
        requireEditable();
        this.title = title;
        this.updatedAt = Instant.now();
    }

    public void setDescription(String description) {
        requireEditable();
        this.description = description;
        this.updatedAt = Instant.now();
    }

    public void setSlug(String slug) {
        requireEditable();
        this.slug = slug;
        this.updatedAt = Instant.now();
    }

    public void setStartsAt(Instant startsAt) {
        requireEditable();
        this.startsAt = startsAt;
        this.updatedAt = Instant.now();
    }

    public void setEndsAt(Instant endsAt) {
        requireEditable();
        this.endsAt = endsAt;
        this.updatedAt = Instant.now();
    }

    public void setNumberRange(int min, int max) {
        requireEditable();
        if (min < 0 || max > 99999 || min > max) {
            throw new IllegalArgumentException("Intervalo de números inválido: " + min + "-" + max);
        }
        this.numberMin = min;
        this.numberMax = max;
        this.updatedAt = Instant.now();
    }

    public void setCurrentRegulationId(Long regulationId) {
        this.currentRegulationId = regulationId;
        this.updatedAt = Instant.now();
    }

    public void transitionTo(CampaignStatus newStatus) {
        CampaignStateMachine.validate(this.status, newStatus);
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    public CapabilityPolicy capabilities() {
        return CapabilityPolicy.forMode(this.mode);
    }

    public int totalNumbers() {
        return numberMax - numberMin + 1;
    }

    private void requireEditable() {
        if (!status.isEditable()) {
            throw ErrorCode.CAMPAIGN_NOT_EDITABLE.toException();
        }
    }
}
