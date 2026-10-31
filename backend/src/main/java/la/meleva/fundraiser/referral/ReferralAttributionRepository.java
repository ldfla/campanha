package la.meleva.fundraiser.referral;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ReferralAttributionRepository extends JpaRepository<ReferralAttribution,Long>{Optional<ReferralAttribution> findByCampaignIdAndEntryId(Long campaignId,Long entryId); long countByReferralLinkId(Long referralLinkId);}
