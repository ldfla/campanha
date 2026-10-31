package la.meleva.fundraiser.payment;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="refunds") public class Refund {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="payment_id",nullable=false) private Long paymentId; @Column(name="provider_refund_id") private String providerRefundId;
 @Column(name="amount_cents",nullable=false) private long amountCents; @Column(nullable=false,length=3) private String currency; @Column(nullable=false,length=30) private String status; @Column(columnDefinition="TEXT") private String reason;
 @Column(name="requested_at",nullable=false) private Instant requestedAt; @Column(name="completed_at") private Instant completedAt;
 protected Refund() {} public Refund(Long paymentId,long amountCents,String reason){if(amountCents<=0)throw new IllegalArgumentException("Estorno deve ser positivo");this.paymentId=paymentId;this.amountCents=amountCents;this.currency="BRL";this.status="PENDING";this.reason=reason;this.requestedAt=Instant.now();} public Long getId(){return id;} public void complete(String providerRefundId){this.providerRefundId=providerRefundId;this.status="COMPLETED";this.completedAt=Instant.now();}
}
