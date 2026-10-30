package la.meleva.fundraiser.referral;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="referral_clicks") public class ReferralClick {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="referral_link_id",nullable=false) private Long referralLinkId; @Column(name="anonymous_id_hash",length=64) private String anonymousIdHash; @Column(name="created_at",nullable=false) private Instant createdAt;
 protected ReferralClick() {} public ReferralClick(Long referralLinkId,String anonymousIdHash){this.referralLinkId=referralLinkId;this.anonymousIdHash=anonymousIdHash;this.createdAt=Instant.now();} public Long getId(){return id;}
}
