package la.meleva.fundraiser.compliance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalDecisionRepository extends JpaRepository<ApprovalDecision, Long> {

    List<ApprovalDecision> findByRequestId(Long requestId);
}
