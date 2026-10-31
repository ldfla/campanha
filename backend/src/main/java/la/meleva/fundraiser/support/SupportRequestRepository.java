package la.meleva.fundraiser.support;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> {
    Optional<SupportRequest> findByPublicId(String publicId);

    List<SupportRequest> findByStatusOrderByIdAsc(SupportRequest.SupportStatus status, Pageable pageable);
}
