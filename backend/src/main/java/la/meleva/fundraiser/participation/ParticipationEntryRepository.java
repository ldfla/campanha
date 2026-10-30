package la.meleva.fundraiser.participation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ParticipationEntryRepository extends JpaRepository<ParticipationEntry, Long> {
    List<ParticipationEntry> findByCampaignIdAndUserId(Long campaignId, Long userId);
}
