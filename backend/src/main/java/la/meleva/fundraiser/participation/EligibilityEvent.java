package la.meleva.fundraiser.participation;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name = "eligibility_events")
public class EligibilityEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="entry_id",nullable=false) private Long entryId; @Column(name="rule_version",nullable=false) private int ruleVersion;
 @Column(nullable=false,length=30) private String result; @Column(name="reason_code",length=100) private String reasonCode;
 @Column(name="checked_at",nullable=false) private Instant checkedAt;
 protected EligibilityEvent() {} public EligibilityEvent(Long entryId,int ruleVersion,String result,String reasonCode){this.entryId=entryId;this.ruleVersion=ruleVersion;this.result=result;this.reasonCode=reasonCode;this.checkedAt=Instant.now();}
 public Long getId(){return id;}
}
