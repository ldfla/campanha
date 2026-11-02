package la.meleva.fundraiser.participation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Page;

public interface CampaignNumberRepository extends JpaRepository<CampaignNumber, Long> {
    Optional<CampaignNumber> findByCampaignIdAndSeriesCodeAndNumberValue(Long campaignId, String seriesCode, int numberValue);
    long countByCampaignIdAndStatus(Long campaignId, NumberStatus status);
    long countByCampaignId(Long campaignId);
    List<CampaignNumber> findByCampaignIdAndStatus(Long campaignId, NumberStatus status);
    List<CampaignNumber> findByEntryId(Long entryId);
    Page<CampaignNumber> findByCampaignId(Long campaignId, org.springframework.data.domain.Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from CampaignNumber n where n.campaignId = :campaignId and n.status = 'AVAILABLE' order by n.numberValue asc")
    List<CampaignNumber> lockAvailableByCampaignId(Long campaignId, org.springframework.data.domain.Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from CampaignNumber n where n.campaignId = :campaignId and n.status = 'AVAILABLE' and n.numberValue >= :start order by n.numberValue asc")
    List<CampaignNumber> lockAvailableFrom(Long campaignId, int start, org.springframework.data.domain.Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from CampaignNumber n where n.campaignId = :campaignId and n.status = 'AVAILABLE' and n.numberValue < :start order by n.numberValue asc")
    List<CampaignNumber> lockAvailableBelow(Long campaignId, int start, org.springframework.data.domain.Pageable pageable);
}
