package la.meleva.fundraiser.drawing;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="draw_rules") public class DrawRule {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="campaign_id",nullable=false) private Long campaignId; @Column(nullable=false) private int version; @Column(name="source_kind",nullable=false) private String sourceKind; @Column(name="formula_json",nullable=false,columnDefinition="json") private String formulaJson; @Column(name="fallback_json",columnDefinition="json") private String fallbackJson; @Column(name="approved_regulation_sha256",length=64) private String approvedRegulationSha256; @Column(name="created_at",nullable=false) private Instant createdAt;
 protected DrawRule() {} public DrawRule(Long campaignId,int version,String sourceKind,String formulaJson,String fallbackJson,String approvedRegulationSha256){this.campaignId=campaignId;this.version=version;this.sourceKind=sourceKind;this.formulaJson=formulaJson;this.fallbackJson=fallbackJson;this.approvedRegulationSha256=approvedRegulationSha256;this.createdAt=Instant.now();} public Long getId(){return id;}
}
