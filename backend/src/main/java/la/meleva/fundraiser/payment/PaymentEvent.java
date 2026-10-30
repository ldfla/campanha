package la.meleva.fundraiser.payment;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="payment_events")
public class PaymentEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="payment_id",nullable=false) private Long paymentId; @Column(name="provider_event_id",nullable=false) private String providerEventId;
 @Column(name="end_to_end_id") private String endToEndId; @Column(name="event_type",nullable=false) private String eventType; @Column(name="payload_sha256",nullable=false,length=64) private String payloadSha256;
 @Column(name="received_at",nullable=false) private Instant receivedAt; @Column(name="processed_at") private Instant processedAt;
 protected PaymentEvent() {} public PaymentEvent(Long paymentId,String providerEventId,String eventType,String payloadSha256){this.paymentId=paymentId;this.providerEventId=providerEventId;this.eventType=eventType;this.payloadSha256=payloadSha256;this.receivedAt=Instant.now();}
 public Long getId(){return id;}
}
