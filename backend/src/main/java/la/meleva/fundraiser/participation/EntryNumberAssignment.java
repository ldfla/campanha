package la.meleva.fundraiser.participation;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "entry_number_assignments")
public class EntryNumberAssignment {
    @EmbeddedId private AssignmentId id;
    @Column(name = "assigned_at", nullable = false) private Instant assignedAt;
    protected EntryNumberAssignment() {}
    public EntryNumberAssignment(Long entryId, Long campaignNumberId) { this.id = new AssignmentId(entryId, campaignNumberId); this.assignedAt = Instant.now(); }
    public AssignmentId getId() { return id; }
    @Embeddable
    public record AssignmentId(@Column(name = "entry_id") Long entryId, @Column(name = "campaign_number_id") Long campaignNumberId) implements Serializable {}
}
