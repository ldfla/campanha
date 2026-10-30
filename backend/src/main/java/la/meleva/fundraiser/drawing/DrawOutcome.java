package la.meleva.fundraiser.drawing;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="draw_outcomes") public class DrawOutcome {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="draw_id",nullable=false) private Long drawId; @Column(name="prize_id",nullable=false) private Long prizeId; @Column(name="winning_number",nullable=false) private int winningNumber; @Column(name="entry_id") private Long entryId; @Column(name="decision_code",nullable=false) private String decisionCode; @Column(name="evidence_hash",length=64) private String evidenceHash; @Column(name="created_at",nullable=false) private Instant createdAt;
 protected DrawOutcome() {} public DrawOutcome(Long drawId,Long prizeId,int winningNumber,Long entryId,String decisionCode,String evidenceHash){if(winningNumber<0||winningNumber>99999)throw new IllegalArgumentException("Número vencedor inválido");this.drawId=drawId;this.prizeId=prizeId;this.winningNumber=winningNumber;this.entryId=entryId;this.decisionCode=decisionCode;this.evidenceHash=evidenceHash;this.createdAt=Instant.now();} public Long getId(){return id;}
}
