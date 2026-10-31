package la.meleva.fundraiser.payment;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="reconciliation_runs") public class ReconciliationRun {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false,length=50) private String provider; @Column(name="started_at",nullable=false) private Instant startedAt; @Column(name="finished_at") private Instant finishedAt; @Column(nullable=false) private int discrepancies; @Column(nullable=false,length=30) private String status;
 protected ReconciliationRun() {} public ReconciliationRun(String provider){this.provider=provider;this.startedAt=Instant.now();this.status="RUNNING";} public Long getId(){return id;}
}
