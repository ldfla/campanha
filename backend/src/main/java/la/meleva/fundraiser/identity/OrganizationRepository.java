package la.meleva.fundraiser.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findByPublicId(String publicId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select o from Organization o where o.id = :id and o.status = :status")
    Optional<Organization> lockForAuthorization(Long id, Organization.OrgStatus status);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select o from Organization o where o.publicId = :publicId and o.status = :status")
    Optional<Organization> lockForAuthorizationByPublicId(String publicId, Organization.OrgStatus status);

    Optional<Organization> findByLegalName(String legalName);
}
