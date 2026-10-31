package la.meleva.fundraiser.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "aggregate_type", nullable = false, length = 50) private String aggregateType;
    @Column(name = "aggregate_id", nullable = false) private Long aggregateId;
    @Column(name = "event_type", nullable = false, length = 100) private String eventType;
    @Column(name = "payload_json", nullable = false, columnDefinition = "json") private String payloadJson;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "available_at", nullable = false) private Instant availableAt;
    @Column(name = "attempt_count", nullable = false) private int attemptCount;
    @Column(name = "max_attempts", nullable = false) private int maxAttempts;
    @Column(name = "locked_at") private Instant lockedAt;
    @Column(name = "locked_by") private String lockedBy;
    @Column(name = "processed_at") private Instant processedAt;
    @Column(name = "error_message", columnDefinition = "TEXT") private String errorMessage;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected OutboxEvent() {}
    public OutboxEvent(String aggregateType, Long aggregateId, String eventType, String payloadJson) {
        this.aggregateType = aggregateType; this.aggregateId = aggregateId; this.eventType = eventType; this.payloadJson = payloadJson;
        this.status = "PENDING"; this.availableAt = Instant.now(); this.attemptCount = 0; this.maxAttempts = 5; this.createdAt = Instant.now();
    }
    public Long getId() { return id; }
}
