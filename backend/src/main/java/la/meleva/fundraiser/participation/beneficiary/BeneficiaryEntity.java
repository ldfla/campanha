package la.meleva.fundraiser.participation.beneficiary;

import jakarta.persistence.*;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "beneficiary_entities")
public class BeneficiaryEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private @Nullable Long id;
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 36) private String publicId;
    @Column(name = "campaign_id", nullable = false, updatable = false) private Long campaignId;
    @Column(nullable = false, length = 150) private String name;
    @Column(nullable = false) private boolean active;

    protected BeneficiaryEntity() { }

    public BeneficiaryEntity(Long campaignId, String name) {
        this.campaignId = Objects.requireNonNull(campaignId, "campaignId");
        this.name = Objects.requireNonNull(name, "name").strip();
        if (this.name.isEmpty() || this.name.length() > 150) throw new IllegalArgumentException("beneficiary.name.invalid");
        this.publicId = UUID.randomUUID().toString();
        this.active = true;
    }
    public Long getId() { return Objects.requireNonNull(id, "beneficiary.id"); }
    public String getPublicId() { return publicId; }
    public String getName() { return name; }
    public void deactivate() { this.active = false; }
}
