package la.meleva.fundraiser.notification;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="inbox_events") public class InboxEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false,length=50) private String source; @Column(name="source_event_id",nullable=false) private String sourceEventId; @Column(name="payload_hash",nullable=false,length=64) private String payloadHash; @Column(name="received_at",nullable=false) private Instant receivedAt; @Column(name="processed_at") private Instant processedAt;
 protected InboxEvent() {} public InboxEvent(String source,String sourceEventId,String payloadHash){this.source=source;this.sourceEventId=sourceEventId;this.payloadHash=payloadHash;this.receivedAt=Instant.now();} public Long getId(){return id;}
}
