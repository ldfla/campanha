package la.meleva.fundraiser.identity;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    @Query("SELECT m FROM Membership m WHERE m.user.id = :userId AND m.status = 'ACTIVE'")
    List<Membership> findActiveByUserId(Long userId);

    @Query("SELECT m FROM Membership m WHERE m.user.id = :userId AND m.organization.id = :orgId AND m.status = 'ACTIVE'")
    List<Membership> findActiveByUserAndOrganization(Long userId, Long orgId);

    boolean existsByUserIdAndOrganizationIdAndRoleAndStatus(
            Long userId, Long organizationId, UserRole role, Membership.MembershipStatus status);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT m FROM Membership m JOIN m.user u WHERE u.id = :userId AND u.status = :userStatus AND m.organization.id = :orgId AND m.status = :membershipStatus AND m.role IN :roles")
    List<Membership> lockActiveForAuthorization(Long userId, Long orgId, List<UserRole> roles,
                                                Membership.MembershipStatus membershipStatus, User.UserStatus userStatus);
}
