package la.meleva.fundraiser.identity;

import la.meleva.fundraiser.audit.AuditEvent;
import la.meleva.fundraiser.audit.AuditEventRepository;
import la.meleva.fundraiser.identity.dto.CreateMembershipRequest;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class IdentityAdministrationService {
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final AuditEventRepository auditEventRepository;

    IdentityAdministrationService(UserRepository userRepository, OrganizationRepository organizationRepository,
                                  MembershipRepository membershipRepository, AuditEventRepository auditEventRepository) {
        this.userRepository = userRepository; this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository; this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public Membership addMembership(Long actorId, CreateMembershipRequest request) {
        var user = userRepository.findByPublicId(request.userPublicId()).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        var organization = organizationRepository.findByPublicId(request.organizationPublicId()).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        if (membershipRepository.existsByUserIdAndOrganizationIdAndRoleAndStatus(user.getId(), organization.getId(), request.role(), Membership.MembershipStatus.ACTIVE)) {
            throw ErrorCode.IDEMPOTENCY_CONFLICT.toException();
        }
        var membership = membershipRepository.save(new Membership(organization, user, request.role()));
        auditEventRepository.save(new AuditEvent(actorId, "MEMBERSHIP_CREATED", "membership", membership.getId(), hash(membership.getId() + ":" + request.role())));
        return membership;
    }

    @Transactional
    public void deactivateMembership(Long actorId, Long membershipId) {
        var membership = membershipRepository.findById(membershipId).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        membership.deactivate(); membershipRepository.save(membership);
        auditEventRepository.save(new AuditEvent(actorId, "MEMBERSHIP_DEACTIVATED", "membership", membershipId, hash(membershipId + ":inactive")));
    }

    private String hash(String value) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
}
