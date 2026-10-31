package la.meleva.fundraiser.participation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    Optional<ParticipationRequest> findByPublicId(String publicId);
    Optional<ParticipationRequest> findByCampaignIdAndUserIdAndIdempotencyKey(Long campaignId, Long userId, String idempotencyKey);
}
