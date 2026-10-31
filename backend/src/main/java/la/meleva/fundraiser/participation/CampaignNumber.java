package la.meleva.fundraiser.participation;

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
import java.util.Objects;

@Entity
@Table(name = "campaign_numbers")
public class CampaignNumber {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "campaign_id", nullable = false) private Long campaignId;
    @Column(name = "series_code", nullable = false, length = 1) private String seriesCode;
    @Column(name = "number_value", nullable = false) private int numberValue;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private NumberStatus status;

    // Colunas de inicialização — cobertas pelo hash de auditoria, imutáveis após a carga.
    @Column(name = "prize_cents") private Long prizeCents;
    @Column(name = "prize_defined_at") private Instant prizeDefinedAt;

    // Colunas de operação — atualizáveis, fora do hash e sem PII.
    @Column(name = "purchased_at") private Instant purchasedAt;
    @Column(name = "purchase_modality", length = 30) private String purchaseModality;
    @Column(name = "correlation_id", length = 36) private String correlationId;
    @Column(name = "charity_institution", length = 150) private String charityInstitution;
    @Column(name = "referrer_phone", length = 20) private String referrerPhone;

    @Column(name = "entry_id") private Long entryId;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version private int version;

    protected CampaignNumber() {}
    public CampaignNumber(Long campaignId, String seriesCode, int numberValue) {
        if (numberValue < 0 || numberValue > 99999) throw new IllegalArgumentException("Número fora do intervalo de cinco dígitos");
        this.campaignId = campaignId; this.seriesCode = seriesCode; this.numberValue = numberValue;
        this.status = NumberStatus.AVAILABLE; this.createdAt = Instant.now(); this.updatedAt = createdAt;
    }
    public Long getId() { return id; }
    public Long getCampaignId() { return campaignId; }
    public String getSeriesCode() { return seriesCode; }
    public int getNumberValue() { return numberValue; }
    public NumberStatus getStatus() { return status; }
    public Long getEntryId() { return entryId; }
    public Long getPrizeCents() { return prizeCents; }
    public Instant getPrizeDefinedAt() { return prizeDefinedAt; }
    public Instant getPurchasedAt() { return purchasedAt; }
    public String getPurchaseModality() { return purchaseModality; }
    public String getCorrelationId() { return correlationId; }
    public String getCharityInstitution() { return charityInstitution; }
    public String getReferrerPhone() { return referrerPhone; }

    public void assignTo(Long entryId) { this.entryId = entryId; this.status = NumberStatus.ASSIGNED; this.updatedAt = Instant.now(); }

    /** Registra a premiação de inicialização. Só pode ser definida uma vez por número. */
    public void definePrize(long cents, Instant definedAt) {
        Objects.requireNonNull(definedAt, "definedAt");
        if (cents <= 0) throw new IllegalArgumentException("Prêmio deve ser positivo");
        if (prizeCents != null || prizeDefinedAt != null) throw new IllegalStateException("number.prize_already_defined");
        this.prizeCents = cents; this.prizeDefinedAt = definedAt; this.updatedAt = definedAt;
    }

    /** Colunas de operação do número (sem PII). A PII do comprador fica em {@link CampaignNumberBuyer}. */
    public void recordSale(Instant purchasedAt, String modality, String correlationId, String charityInstitution, String referrerPhone) {
        if (charityInstitution == null || charityInstitution.isBlank()) {
            throw new IllegalArgumentException("number.charity_required");
        }
        this.purchasedAt = purchasedAt; this.purchaseModality = modality; this.correlationId = correlationId;
        this.charityInstitution = charityInstitution.strip();
        this.referrerPhone = la.meleva.fundraiser.shared.PhoneNumbers.optionalE164(referrerPhone);
        this.updatedAt = purchasedAt == null ? this.updatedAt : purchasedAt;
    }

    public void reserveFor(Long entryId) {
        Objects.requireNonNull(entryId, "entryId");
        if (status != NumberStatus.AVAILABLE) throw new IllegalStateException("number.not_available");
        this.entryId = entryId; this.status = NumberStatus.RESERVED; this.updatedAt = Instant.now();
    }
    public void releaseReservation(Long expectedEntryId) {
        Objects.requireNonNull(expectedEntryId, "expectedEntryId");
        if (status != NumberStatus.RESERVED || !expectedEntryId.equals(entryId)) throw new IllegalStateException("number.reservation_mismatch");
        this.entryId = null; this.status = NumberStatus.AVAILABLE; this.updatedAt = Instant.now();
    }
    public void assignReservation(Long expectedEntryId) {
        Objects.requireNonNull(expectedEntryId, "expectedEntryId");
        if (status != NumberStatus.RESERVED || !expectedEntryId.equals(entryId)) throw new IllegalStateException("number.reservation_mismatch");
        this.status = NumberStatus.ASSIGNED; this.updatedAt = Instant.now();
    }
    public void freeze() { if (status == NumberStatus.ASSIGNED) { this.status = NumberStatus.FROZEN; this.updatedAt = Instant.now(); } }
    public void invalidate() { if (status == NumberStatus.FROZEN) { this.status = NumberStatus.INVALIDATED; this.updatedAt = Instant.now(); } }
}
