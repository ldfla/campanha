package la.meleva.fundraiser.compliance;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="campaign_regulations") public class CampaignRegulation {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="campaign_id",nullable=false) private Long campaignId; @Column(nullable=false) private int version; @Column(name="document_sha256",nullable=false,length=64) private String documentSha256; @Column(name="document_uri") private String documentUri; @Column(name="surprise_required",nullable=false) private boolean surpriseRequired; @Column(name="approved_at") private Instant approvedAt; @Column(name="approval_status",nullable=false) private String approvalStatus; @Column(name="created_at",nullable=false) private Instant createdAt;
 protected CampaignRegulation() {}
 public CampaignRegulation(Long campaignId,int version,String documentSha256,String documentUri){this(campaignId,version,documentSha256,documentUri,false);}
 public CampaignRegulation(Long campaignId,int version,String documentSha256,String documentUri,boolean surpriseRequired){this.campaignId=campaignId;this.version=version;this.documentSha256=documentSha256;this.documentUri=documentUri;this.surpriseRequired=surpriseRequired;this.approvalStatus="PENDING";this.createdAt=Instant.now();}
 public Long getId(){return id;} public int getVersion(){return version;} public String getDocumentSha256(){return documentSha256;} public boolean isSurpriseRequired(){return surpriseRequired;}
}
