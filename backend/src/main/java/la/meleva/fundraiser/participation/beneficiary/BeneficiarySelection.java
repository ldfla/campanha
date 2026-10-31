package la.meleva.fundraiser.participation.beneficiary;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "beneficiary_selections")
public class BeneficiarySelection {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "request_id", nullable = false, unique = true, updatable = false) private Long requestId;
    @Column(name = "beneficiary_id", nullable = false, updatable = false) private Long beneficiaryId;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected BeneficiarySelection() { }

    public BeneficiarySelection(Long requestId, Long beneficiaryId) {
        this.requestId = Objects.requireNonNull(requestId, "requestId");
        this.beneficiaryId = Objects.requireNonNull(beneficiaryId, "beneficiaryId");
        this.createdAt = Instant.now();
    }
    public Long getBeneficiaryId() { return beneficiaryId; }
}
