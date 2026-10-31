package la.meleva.fundraiser.drawing;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FederalResultReviewRepository extends JpaRepository<FederalResultReview, Long> {
    List<FederalResultReview> findByFederalResultId(Long federalResultId);
    boolean existsByFederalResultIdAndReviewerUserId(Long federalResultId, Long reviewerUserId);
    long countByFederalResultId(Long federalResultId);
}
