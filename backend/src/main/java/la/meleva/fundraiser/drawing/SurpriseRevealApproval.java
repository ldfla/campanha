package la.meleva.fundraiser.drawing;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name="surprise_reveal_approvals")
public class SurpriseRevealApproval {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="commitment_id",nullable=false,updatable=false) private Long commitmentId;
    @Column(name="approver_user_id",nullable=false,updatable=false) private Long approverUserId;
    @Enumerated(EnumType.STRING) @Column(name="approval_role",nullable=false,updatable=false,length=30) private ApprovalRole role;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;

    protected SurpriseRevealApproval() {}
    public SurpriseRevealApproval(Long commitmentId,Long approverUserId,ApprovalRole role){
        this.commitmentId=Objects.requireNonNull(commitmentId,"commitmentId");
        this.approverUserId=Objects.requireNonNull(approverUserId,"approverUserId");
        this.role=Objects.requireNonNull(role,"role");
        this.createdAt=Instant.now();
    }
    public Long getId(){return id;}
    public Long getApproverUserId(){return approverUserId;}
    public ApprovalRole getRole(){return role;}
    public enum ApprovalRole { DRAW_AUDITOR, COMPLIANCE_REVIEWER }
}
