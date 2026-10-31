package la.meleva.fundraiser.participation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ParticipationEntryRepository extends JpaRepository<ParticipationEntry, Long> {
    List<ParticipationEntry> findByCampaignIdAndUserId(Long campaignId, Long userId);
    List<ParticipationEntry> findByRequestId(Long requestId);
    List<ParticipationEntry> findByCampaignIdAndStatus(Long campaignId, ParticipationStatus status);
    long countByCampaignIdAndUserId(Long campaignId, Long userId);
    long countByCampaignIdAndUserIdAndStatusNotIn(Long campaignId, Long userId, java.util.Collection<ParticipationStatus> statuses);
}
