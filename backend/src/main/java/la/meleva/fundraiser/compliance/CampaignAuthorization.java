package la.meleva.fundraiser.compliance;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import la.meleva.fundraiser.shared.ErrorCode;

@Entity @Table(name="campaign_authorizations") public class CampaignAuthorization {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="campaign_id",nullable=false) private Long campaignId; private String issuer; @Column(name="certificate_no") private String certificateNo; @Column(name="valid_from") private Instant validFrom; @Column(name="valid_to") private Instant validTo; @Column(name="approval_scope_json",columnDefinition="json") private String approvalScopeJson; @Column(name="document_uri") private String documentUri; @Column(name="verified_at") private Instant verifiedAt; @Column(nullable=false) private String status; @Column(name="created_at",nullable=false) private Instant createdAt; @Column(name="updated_at",nullable=false) private Instant updatedAt;

 protected CampaignAuthorization() {}
 public CampaignAuthorization(Long campaignId,String issuer,String certificateNo,Instant validFrom,Instant validTo,String documentUri){
   if(validFrom!=null&&validTo!=null&&validTo.isBefore(validFrom))throw ErrorCode.VALIDATION_ERROR.toException();
   this.campaignId=campaignId;this.issuer=issuer;this.certificateNo=certificateNo;this.validFrom=validFrom;this.validTo=validTo;this.documentUri=documentUri;this.status="PENDING";this.createdAt=Instant.now();this.updatedAt=createdAt;
 }
 public Long getId(){return id;}
 public Long getCampaignId(){return campaignId;}
 public String getIssuer(){return issuer;}
 public String getCertificateNo(){return certificateNo;}
 public Instant getValidFrom(){return validFrom;}
 public Instant getValidTo(){return validTo;}
 public Instant getVerifiedAt(){return verifiedAt;}
 public String getStatus(){return status;}

 /** Verificação (dupla aprovação é responsabilidade do fluxo de compliance). */
 public void verify(Instant at){ if(verifiedAt!=null) throw ErrorCode.APPROVAL_ALREADY_DECIDED.toException(); this.status="VERIFIED"; this.verifiedAt=Objects.requireNonNull(at,"at"); this.updatedAt=at; }

 /** Autorização utilizável: verificada e dentro da vigência (ausência de datas = sem limite). */
 public boolean isEffectiveAt(Instant at){
   if(verifiedAt==null) return false;
   if(validFrom!=null && at.isBefore(validFrom)) return false;
   if(validTo!=null && at.isAfter(validTo)) return false;
   return true;
 }
}
