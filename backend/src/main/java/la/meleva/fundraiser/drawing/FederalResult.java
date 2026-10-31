package la.meleva.fundraiser.drawing;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="federal_results") public class FederalResult {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="contest_identifier",nullable=false) private String contestIdentifier; @Column(name="drawn_at") private Instant drawnAt; @Column(name="source_url",length=1024) private String sourceUrl; @Column(name="source_document_hash",length=64) private String sourceDocumentHash; @Column(name="result_payload",nullable=false,columnDefinition="json") private String resultPayload; @Column(name="verified_by_two",nullable=false) private boolean verifiedByTwo; @Column(name="verified_at") private Instant verifiedAt; @Column(name="created_at",nullable=false) private Instant createdAt;
 protected FederalResult() {} public FederalResult(String contestIdentifier,String resultPayload){this.contestIdentifier=contestIdentifier;this.resultPayload=resultPayload;this.createdAt=Instant.now();} public Long getId(){return id;}
}
