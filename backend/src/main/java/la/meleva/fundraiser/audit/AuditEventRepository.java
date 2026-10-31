package la.meleva.fundraiser.audit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AuditEventRepository extends JpaRepository<AuditEvent,Long>{List<AuditEvent> findByEntityTypeAndEntityIdOrderByCreatedAt(String entityType,Long entityId);}
