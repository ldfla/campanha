package la.meleva.fundraiser.drawing;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import la.meleva.fundraiser.shared.ErrorCode;

@Entity @Table(name="federal_results") public class FederalResult {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="contest_identifier",nullable=false) private String contestIdentifier;
 @Column(name="source_kind",length=50) private String sourceKind;
 @Column(name="drawn_at") private Instant drawnAt;
 @Column(name="source_url",length=1024) private String sourceUrl;
 @Column(name="source_document_hash",length=64) private String sourceDocumentHash;
 @Column(name="result_payload",nullable=false,columnDefinition="json") private String resultPayload;
 @Column(name="verified_by_two",nullable=false) private boolean verifiedByTwo;
 @Column(name="verified_at") private Instant verifiedAt;
 @Column(name="created_at",nullable=false) private Instant createdAt;

 protected FederalResult() {}

 public FederalResult(String contestIdentifier,String sourceKind,Instant drawnAt,String sourceUrl,String sourceDocumentHash,String resultPayload){
  this.contestIdentifier=Objects.requireNonNull(contestIdentifier,"contestIdentifier");
  this.sourceKind=sourceKind; this.drawnAt=drawnAt; this.sourceUrl=sourceUrl;
  this.sourceDocumentHash=Objects.requireNonNull(sourceDocumentHash,"sourceDocumentHash");
  this.resultPayload=Objects.requireNonNull(resultPayload,"resultPayload");
  this.verifiedByTwo=false; this.createdAt=Instant.now();
 }
 public Long getId(){return id;}
 public String getContestIdentifier(){return contestIdentifier;}
 public String getSourceKind(){return sourceKind;}
 public Instant getDrawnAt(){return drawnAt;}
 public String getSourceUrl(){return sourceUrl;}
 public String getSourceDocumentHash(){return sourceDocumentHash;}
 public String getResultPayload(){return resultPayload;}
 public boolean isVerifiedByTwo(){return verifiedByTwo;}
 public Instant getVerifiedAt(){return verifiedAt;}
 public void markVerified(Instant at){ if(verifiedByTwo) throw ErrorCode.APPROVAL_ALREADY_DECIDED.toException(); this.verifiedByTwo=true; this.verifiedAt=at; }
}
