package la.meleva.fundraiser.drawing;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

/** Revisão independente do resultado oficial (P12). Duas revisões distintas liberam a apuração. */
@Entity @Table(name="federal_result_reviews") public class FederalResultReview {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="federal_result_id",nullable=false,updatable=false) private Long federalResultId;
 @Column(name="reviewer_user_id",nullable=false,updatable=false) private Long reviewerUserId;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;

 protected FederalResultReview() {}
 public FederalResultReview(Long federalResultId,Long reviewerUserId){
  this.federalResultId=Objects.requireNonNull(federalResultId,"federalResultId");
  this.reviewerUserId=Objects.requireNonNull(reviewerUserId,"reviewerUserId");
  this.createdAt=Instant.now();
 }
 public Long getId(){return id;}
 public Long getFederalResultId(){return federalResultId;}
 public Long getReviewerUserId(){return reviewerUserId;}
}
