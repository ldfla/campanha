package la.meleva.fundraiser.referral;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ReferralLinkRepository extends JpaRepository<ReferralLink,Long>{Optional<ReferralLink> findByCode(String code); Optional<ReferralLink> findByCampaignIdAndPromoterUserId(Long campaignId, Long promoterUserId);}
