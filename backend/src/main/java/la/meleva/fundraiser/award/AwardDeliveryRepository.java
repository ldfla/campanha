package la.meleva.fundraiser.award;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AwardDeliveryRepository extends JpaRepository<AwardDelivery,Long>{List<AwardDelivery> findByBeneficiaryUserId(Long beneficiaryUserId);}
