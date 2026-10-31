package la.meleva.fundraiser.referral;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="referral_links") public class ReferralLink {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="campaign_id",nullable=false) private Long campaignId; @Column(name="promoter_user_id",nullable=false) private Long promoterUserId;
 @Column(nullable=false,length=20) private String code; @Column(nullable=false,length=30) private String status; @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 protected ReferralLink() {} public ReferralLink(Long campaignId,Long promoterUserId,String code){this.campaignId=campaignId;this.promoterUserId=promoterUserId;this.code=code;this.status="ACTIVE";this.createdAt=Instant.now();} public Long getId(){return id;} public String getCode(){return code;}
}
