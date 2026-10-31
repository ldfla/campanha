package la.meleva.fundraiser.campaign;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    Optional<Campaign> findByPublicId(String publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Campaign> findLockedByPublicId(String publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Campaign> findLockedById(Long id);

    Optional<Campaign> findBySlug(String slug);

    List<Campaign> findByOrganizationId(Long organizationId);

    List<Campaign> findByOrganizationIdAndStatus(Long organizationId, CampaignStatus status);

    boolean existsBySlug(String slug);
}
