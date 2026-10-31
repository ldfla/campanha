package la.meleva.fundraiser.payment;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "orders")
public class Order {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="public_id",nullable=false,updatable=false) private String publicId;
 @Column(name="campaign_id",nullable=false) private Long campaignId; @Column(name="user_id",nullable=false) private Long userId;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private OrderStatus status;
 @Column(name="total_cents",nullable=false) private long totalCents; @Column(nullable=false,length=3) private String currency;
 @Column(nullable=false,length=30) private String mode; @Column(name="expires_at") private Instant expiresAt;
 @Column(name="idempotency_key",nullable=false) private String idempotencyKey;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt; @Column(name="updated_at",nullable=false) private Instant updatedAt; @Version private int version;
 protected Order() {}
 public Order(Long campaignId,Long userId,long totalCents,String mode,String idempotencyKey,Instant expiresAt) {
  if(totalCents<0) throw new IllegalArgumentException("Valor do pedido não pode ser negativo");
  this.publicId=UUID.randomUUID().toString();this.campaignId=campaignId;this.userId=userId;this.totalCents=totalCents;this.currency="BRL";this.mode=mode;this.idempotencyKey=idempotencyKey;this.expiresAt=expiresAt;this.status=OrderStatus.CREATED;this.createdAt=Instant.now();this.updatedAt=createdAt;
 }
 public Long getId(){return id;} public Long getCampaignId(){return campaignId;} public Long getUserId(){return userId;} public long getTotalCents(){return totalCents;} public OrderStatus getStatus(){return status;}
}
