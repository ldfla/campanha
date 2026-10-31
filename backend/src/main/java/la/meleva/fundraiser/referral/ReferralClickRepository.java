package la.meleva.fundraiser.referral;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ReferralClickRepository extends JpaRepository<ReferralClick,Long>{long countByReferralLinkId(Long referralLinkId);}
