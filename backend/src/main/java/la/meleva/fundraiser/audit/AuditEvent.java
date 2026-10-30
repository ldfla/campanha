package la.meleva.fundraiser.audit;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="audit_events") public class AuditEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="actor_id") private Long actorId; @Column(nullable=false) private String action; @Column(name="entity_type",nullable=false) private String entityType; @Column(name="entity_id",nullable=false) private Long entityId; @Column(name="before_hash",length=64) private String beforeHash; @Column(name="after_hash",length=64) private String afterHash; @Column(name="correlation_id",length=36) private String correlationId; @Column(name="chain_prev_hash",length=64) private String chainPrevHash; @Column(name="event_hash",nullable=false,length=64) private String eventHash; @Column(name="created_at",nullable=false) private Instant createdAt;
 protected AuditEvent() {} public AuditEvent(Long actorId,String action,String entityType,Long entityId,String eventHash){this.actorId=actorId;this.action=action;this.entityType=entityType;this.entityId=entityId;this.eventHash=eventHash;this.createdAt=Instant.now();} public Long getId(){return id;}
}
