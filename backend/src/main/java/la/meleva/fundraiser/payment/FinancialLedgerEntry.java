package la.meleva.fundraiser.payment;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="financial_ledger") public class FinancialLedgerEntry {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="campaign_id",nullable=false) private Long campaignId; @Column(name="source_ref",nullable=false) private String sourceRef; @Column(name="entry_type",nullable=false) private String entryType;
 @Column(name="amount_cents",nullable=false) private long amountCents; @Column(nullable=false,length=3) private String currency; @Column(name="posted_at",nullable=false) private Instant postedAt;
 protected FinancialLedgerEntry() {} public FinancialLedgerEntry(Long campaignId,String sourceRef,String entryType,long amountCents){this.campaignId=campaignId;this.sourceRef=sourceRef;this.entryType=entryType;this.amountCents=amountCents;this.currency="BRL";this.postedAt=Instant.now();} public Long getId(){return id;} public String getSourceRef(){return sourceRef;}
}
