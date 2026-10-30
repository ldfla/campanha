package la.meleva.fundraiser.identity;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    @Query("SELECT m FROM Membership m WHERE m.user.id = :userId AND m.status = 'ACTIVE'")
    List<Membership> findActiveByUserId(Long userId);

    @Query("SELECT m FROM Membership m WHERE m.user.id = :userId AND m.organization.id = :orgId AND m.status = 'ACTIVE'")
    List<Membership> findActiveByUserAndOrganization(Long userId, Long orgId);

    boolean existsByUserIdAndOrganizationIdAndRoleAndStatus(
            Long userId, Long organizationId, UserRole role, Membership.MembershipStatus status);
}
