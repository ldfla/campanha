package la.meleva.fundraiser.support;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> {
    Optional<SupportRequest> findByPublicId(String publicId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from SupportRequest r where r.publicId = :publicId")
    Optional<SupportRequest> lockByPublicId(String publicId);

    List<SupportRequest> findByUserIdAndStatusOrderByIdAsc(Long userId, SupportRequest.SupportStatus status, Pageable pageable);
    List<SupportRequest> findByOrganizationIdAndStatusOrderByIdAsc(Long organizationId, SupportRequest.SupportStatus status, Pageable pageable);
}
