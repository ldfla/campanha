package la.meleva.fundraiser.referral;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="referral_attributions") public class ReferralAttribution {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="campaign_id",nullable=false) private Long campaignId; @Column(name="referral_link_id",nullable=false) private Long referralLinkId; @Column(name="referred_user_id",nullable=false) private Long referredUserId; @Column(name="entry_id") private Long entryId; @Column(nullable=false,length=30) private String status; @Column(name="attributed_at") private Instant attributedAt; @Column(name="created_at",nullable=false) private Instant createdAt;
 protected ReferralAttribution() {} public ReferralAttribution(Long campaignId,Long referralLinkId,Long referredUserId,Long entryId){this.campaignId=campaignId;this.referralLinkId=referralLinkId;this.referredUserId=referredUserId;this.entryId=entryId;this.status="PENDING";this.createdAt=Instant.now();} public Long getId(){return id;}
}
