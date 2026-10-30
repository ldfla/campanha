package la.meleva.fundraiser.participation;
import org.springframework.data.jpa.repository.JpaRepository;
public interface EntryNumberAssignmentRepository extends JpaRepository<EntryNumberAssignment, EntryNumberAssignment.AssignmentId> {}
