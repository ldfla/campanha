package la.meleva.fundraiser.payment;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="payments")
public class Payment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="order_id",nullable=false) private Long orderId;
 @Column(nullable=false,length=50) private String provider; @Column(name="provider_charge_id") private String providerChargeId; private String txid;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private PaymentStatus status; @Column(name="amount_cents",nullable=false) private long amountCents; @Column(nullable=false,length=3) private String currency;
 @Column(name="confirmed_at") private Instant confirmedAt; @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt; @Column(name="updated_at",nullable=false) private Instant updatedAt;
 protected Payment() {} public Payment(Long orderId,String provider,long amountCents){if(amountCents<0)throw new IllegalArgumentException("Valor do pagamento não pode ser negativo");this.orderId=orderId;this.provider=provider;this.amountCents=amountCents;this.currency="BRL";this.status=PaymentStatus.PENDING;this.createdAt=Instant.now();this.updatedAt=createdAt;}
 public Long getId(){return id;} public Long getOrderId(){return orderId;} public PaymentStatus getStatus(){return status;} public String getProvider(){return provider;} public String getProviderChargeId(){return providerChargeId;} public String getTxid(){return txid;} public long getAmountCents(){return amountCents;}
 public void bindCharge(String chargeId,String txid){this.providerChargeId=chargeId;this.txid=txid;this.updatedAt=Instant.now();}
 public void confirm(){this.status=PaymentStatus.CONFIRMED;this.confirmedAt=Instant.now();this.updatedAt=confirmedAt;}
 public void expire(){this.status=PaymentStatus.EXPIRED;this.updatedAt=Instant.now();}
 public void fail(){this.status=PaymentStatus.FAILED;this.updatedAt=Instant.now();}
 public void refund(){this.status=PaymentStatus.REFUNDED;this.updatedAt=Instant.now();}
}
