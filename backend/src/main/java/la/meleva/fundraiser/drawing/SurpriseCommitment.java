package la.meleva.fundraiser.drawing;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="surprise_commitments") public class SurpriseCommitment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="campaign_id",nullable=false) private Long campaignId; @Column(name="commitment_sha256",nullable=false,length=64) private String commitmentSha256; @Column(name="encrypted_secret_object_key",length=512) private String encryptedSecretObjectKey; @Column(name="committed_at",nullable=false) private Instant committedAt; @Column(name="revealed_at") private Instant revealedAt; @Column(name="rule_version",nullable=false) private int ruleVersion; @Column(name="created_at",nullable=false) private Instant createdAt;
 protected SurpriseCommitment() {} public SurpriseCommitment(Long campaignId,String commitmentSha256,String encryptedSecretObjectKey,int ruleVersion){this.campaignId=campaignId;this.commitmentSha256=commitmentSha256;this.encryptedSecretObjectKey=encryptedSecretObjectKey;this.ruleVersion=ruleVersion;this.committedAt=Instant.now();this.createdAt=committedAt;} public Long getId(){return id;}
}
