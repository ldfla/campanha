package la.meleva.fundraiser.payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;
public interface PaymentRepository extends JpaRepository<Payment,Long>{
 Optional<Payment> findByProviderAndProviderChargeId(String provider,String providerChargeId);
 Optional<Payment> findByProviderAndTxid(String provider,String txid);
 Optional<Payment> findByOrderId(Long orderId);
 @Lock(LockModeType.PESSIMISTIC_WRITE) Optional<Payment> findLockedByOrderId(Long orderId);
 @Lock(LockModeType.PESSIMISTIC_WRITE) Optional<Payment> findLockedById(Long id);
 @Lock(LockModeType.PESSIMISTIC_WRITE) Optional<Payment> findLockedByProviderAndProviderChargeId(String provider,String providerChargeId);
}
