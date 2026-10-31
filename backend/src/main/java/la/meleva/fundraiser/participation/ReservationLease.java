package la.meleva.fundraiser.participation;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="reservation_leases")
public class ReservationLease {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="campaign_id",nullable=false) private Long campaignId;
 @Column(name="number_id",nullable=false) private Long numberId; @Column(name="request_id",nullable=false) private Long requestId;
 @Column(name="expires_at",nullable=false) private Instant expiresAt; @Column(nullable=false,length=30) private String status;
 @Version private int version; @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 protected ReservationLease() {} public ReservationLease(Long campaignId,Long numberId,Long requestId,Instant expiresAt){this.campaignId=campaignId;this.numberId=numberId;this.requestId=requestId;this.expiresAt=expiresAt;this.status="ACTIVE";this.createdAt=Instant.now();}
 public Long getId(){return id;}
}
