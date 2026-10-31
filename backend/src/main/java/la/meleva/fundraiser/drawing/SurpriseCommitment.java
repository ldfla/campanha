package la.meleva.fundraiser.drawing;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="surprise_commitments") public class SurpriseCommitment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="campaign_id",nullable=false) private Long campaignId; @Column(name="commitment_sha256",nullable=false,length=64) private String commitmentSha256; @Column(name="encrypted_secret_object_key",length=512) private String encryptedSecretObjectKey; @Column(name="committed_at",nullable=false) private Instant committedAt; @Column(name="revealed_at") private Instant revealedAt; @Column(name="rule_version",nullable=false) private int ruleVersion; @Column(name="created_at",nullable=false) private Instant createdAt;
 @Column(name="encrypted_secret", columnDefinition="TEXT") private String encryptedSecret;
 @Column(name="proof_number_min",updatable=false) private Integer proofNumberMin;
 @Column(name="proof_number_max",updatable=false) private Integer proofNumberMax;
 @Column(name="proof_regulation_version",updatable=false) private Integer proofRegulationVersion;
 @Column(name="created_by",updatable=false) private Long createdBy;
 protected SurpriseCommitment() {} public SurpriseCommitment(Long campaignId,String commitmentSha256,String encryptedSecretObjectKey,int ruleVersion){this.campaignId=campaignId;this.commitmentSha256=commitmentSha256;this.encryptedSecretObjectKey=encryptedSecretObjectKey;this.ruleVersion=ruleVersion;this.committedAt=Instant.now();this.createdAt=committedAt;} public Long getId(){return id;}
 public SurpriseCommitment(Long campaignId,String hash,SurpriseProof.Context context,Long actorId){this.campaignId=campaignId;this.commitmentSha256=hash;this.ruleVersion=context.ruleVersion();this.committedAt=context.committedAt();this.createdAt=committedAt;this.proofNumberMin=context.numberMin();this.proofNumberMax=context.numberMax();this.proofRegulationVersion=context.regulationVersion();this.createdBy=java.util.Objects.requireNonNull(actorId,"actorId");}
 public Long getCreatedBy(){return createdBy;}
 public Integer getProofNumberMin(){return proofNumberMin;} public Integer getProofNumberMax(){return proofNumberMax;} public Integer getProofRegulationVersion(){return proofRegulationVersion;}
 public String getCommitmentSha256(){return commitmentSha256;} public String getEncryptedSecret(){return encryptedSecret;} public void setEncryptedSecret(String value){this.encryptedSecret=value;} public int getRuleVersion(){return ruleVersion;} public Instant getCommittedAt(){return committedAt;} public boolean isRevealed(){return revealedAt!=null;} public void reveal(){if(revealedAt==null)revealedAt=Instant.now();}
}
