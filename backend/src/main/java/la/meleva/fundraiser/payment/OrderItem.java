package la.meleva.fundraiser.payment;
import jakarta.persistence.*;
@Entity @Table(name="order_items")
public class OrderItem {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="order_id",nullable=false) private Long orderId;
 @Column(nullable=false) private String description; @Column(nullable=false) private int quantity; @Column(name="unit_amount_cents",nullable=false) private long unitAmountCents;
 @Column(nullable=false,length=3) private String currency; @Column(name="reference_id") private Long referenceId;
 protected OrderItem() {} public OrderItem(Long orderId,String description,int quantity,long unitAmountCents,Long referenceId){if(quantity<1||unitAmountCents<0)throw new IllegalArgumentException("Item inválido");this.orderId=orderId;this.description=description;this.quantity=quantity;this.unitAmountCents=unitAmountCents;this.currency="BRL";this.referenceId=referenceId;}
 public Long getId(){return id;}
}
