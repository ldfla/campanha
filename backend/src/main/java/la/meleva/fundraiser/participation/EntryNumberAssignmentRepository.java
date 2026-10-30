package la.meleva.fundraiser.participation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EntryNumberAssignmentRepository extends JpaRepository<EntryNumberAssignment, EntryNumberAssignment.AssignmentId> {
    Optional<EntryNumberAssignment> findByIdEntryId(Long entryId);
}
