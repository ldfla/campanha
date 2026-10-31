package la.meleva.fundraiser.drawing;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SurpriseRevealApprovalRepository extends JpaRepository<SurpriseRevealApproval,Long> {
    boolean existsByCommitmentIdAndApproverUserId(Long commitmentId,Long approverUserId);
    boolean existsByCommitmentIdAndRole(Long commitmentId,SurpriseRevealApproval.ApprovalRole role);
    long countByCommitmentId(Long commitmentId);
    java.util.List<SurpriseRevealApproval> findByCommitmentId(Long commitmentId);
    java.util.Optional<SurpriseRevealApproval> findByCommitmentIdAndApproverUserId(Long commitmentId,Long approverUserId);
}
