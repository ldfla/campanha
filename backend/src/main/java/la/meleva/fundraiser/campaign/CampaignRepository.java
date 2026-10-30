package la.meleva.fundraiser.campaign;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    Optional<Campaign> findByPublicId(String publicId);

    Optional<Campaign> findBySlug(String slug);

    List<Campaign> findByOrganizationId(Long organizationId);

    List<Campaign> findByOrganizationIdAndStatus(Long organizationId, CampaignStatus status);

    boolean existsBySlug(String slug);
}
