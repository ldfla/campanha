package la.meleva.fundraiser.payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface PaymentRepository extends JpaRepository<Payment,Long>{Optional<Payment> findByProviderAndProviderChargeId(String provider,String providerChargeId); Optional<Payment> findByProviderAndTxid(String provider,String txid);}
