package la.meleva.fundraiser.support;

import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Objects;
import la.meleva.fundraiser.audit.AuditEvent;
import la.meleva.fundraiser.audit.AuditEventRepository;
import la.meleva.fundraiser.identity.MembershipRepository;
import la.meleva.fundraiser.identity.Membership;
import la.meleva.fundraiser.identity.Organization;
import la.meleva.fundraiser.identity.OrganizationRepository;
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.identity.UserRole;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Suporte interno: consultas do autor ou de operadores vinculados à organização do chamado. */
@Service
public class SupportService {

    private static final int MAX_PAGE = 100;

    private final SupportRequestRepository requests;
    private final UserRepository users;
    private final MembershipRepository memberships;
    private final OrganizationRepository organizations;
    private final AuditEventRepository audits;
    private final Clock clock;

    @Autowired
    SupportService(SupportRequestRepository requests, UserRepository users, MembershipRepository memberships,
                   OrganizationRepository organizations, AuditEventRepository audits) {
        this(requests, users, memberships, organizations, audits, Clock.systemUTC());
    }

    SupportService(SupportRequestRepository requests, UserRepository users, MembershipRepository memberships,
                   OrganizationRepository organizations, AuditEventRepository audits, Clock clock) {
        this.requests = requests;
        this.users = users;
        this.memberships = memberships;
        this.organizations = organizations;
        this.audits = audits;
        this.clock = clock;
    }

    @Transactional
    public SupportView open(Long userId, String subject, String message, String organizationId, String idempotencyKey) {
        if (userId == null) throw ErrorCode.VALIDATION_ERROR.toException();
        String key = canonicalKey(idempotencyKey);
        var user = (key == null ? users.findById(userId) : users.findLockedById(userId)).filter(candidate -> candidate.isActive())
                .orElseThrow(ErrorCode.AUTH_RESOURCE_ACCESS_DENIED::toException);
        if (subject == null || subject.isBlank() || subject.strip().length() > 150
                || message == null || message.isBlank() || message.strip().length() > 2000) {
            throw ErrorCode.VALIDATION_ERROR.toException();
        }
        String fingerprint = key == null ? null : fingerprint(subject.strip(), message.strip(), organizationId);
        if (key != null) {
            var previous = requests.findByUserIdAndIdempotencyKey(user.getId(), key);
            if (previous.isPresent()) {
                var existing = previous.get();
                if (!fingerprint.equals(existing.getIdempotencyFingerprint()))
                    throw ErrorCode.IDEMPOTENCY_CONFLICT.toException();
                String currentOrganization = existing.getOrganizationId() == null ? null
                        : organizations.findById(existing.getOrganizationId()).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException).getPublicId();
                return view(existing, currentOrganization);
            }
        }
        var request = new SupportRequest(user.getId(), subject.strip(), message.strip());
        var organization = organizationId == null ? null : activeOrganization(organizationId);
        if (organization != null) request.assignOrganization(organization.getId());
        if (key != null) request.setIdempotency(key, fingerprint);
        var saved = requests.save(request);
        audits.save(new AuditEvent(user.getId(), "SUPPORT_REQUEST_OPENED", "support_request", saved.getId(),
                sha256(saved.getPublicId() + ":" + (organization == null ? "unassigned" : organization.getPublicId()))));
        return view(saved, organization == null ? null : organization.getPublicId());
    }

    @Transactional
    public List<SupportView> list(Long actorId, String organizationId, int page, int size, SupportRequest.SupportStatus status) {
        if (page < 0 || size < 1 || size > MAX_PAGE || status == null) throw ErrorCode.VALIDATION_ERROR.toException();
        var actor = activeUser(actorId);
        List<SupportRequest> entries;
        if (organizationId == null) {
            entries = requests.findByUserIdAndStatusOrderByIdAsc(actor.getId(), status,
                    PageRequest.of(page, size));
        } else {
            var organization = activeOrganization(organizationId);
            requireOperationalRole(actor.getId(), organization.getId());
            entries = requests.findByOrganizationIdAndStatusOrderByIdAsc(organization.getId(), status, PageRequest.of(page, size));
        }
        var ids = entries.stream().map(SupportRequest::getOrganizationId).filter(Objects::nonNull).distinct().toList();
        Map<Long, Organization> byId = organizations.findAllById(ids).stream()
                .collect(Collectors.toMap(Organization::getId, Function.identity()));
        return entries.stream().map(entry -> view(entry, entry.getOrganizationId() == null ? null
                : byId.get(entry.getOrganizationId()).getPublicId())).toList();
    }

    @Transactional
    public SupportView transition(Long actorId, String requestId, SupportRequest.SupportStatus next) {
        if (next == null) throw ErrorCode.VALIDATION_ERROR.toException();
        var actor = activeUser(actorId);
        var organizationId = requests.findOrganizationIdByPublicId(requestId)
                .orElseThrow(ErrorCode.AUTH_RESOURCE_ACCESS_DENIED::toException);
        var organization = organizations.lockForAuthorization(organizationId, Organization.OrgStatus.ACTIVE)
                .orElseThrow(ErrorCode.AUTH_RESOURCE_ACCESS_DENIED::toException);
        requireOperationalRole(actor.getId(), organization.getId());
        var request = requests.lockByPublicId(requestId).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        if (!organization.getId().equals(request.getOrganizationId()))
            throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        var previous = request.getStatus();
        if (request.transitionTo(next, clock.instant())) {
            audits.save(new AuditEvent(actor.getId(), "SUPPORT_REQUEST_STATUS_CHANGED", "support_request", request.getId(),
                    sha256("support-status-v1:" + request.getPublicId() + ":" + organization.getPublicId()
                            + ":" + previous.name() + ":" + next.name())));
        }
        return view(request, organization.getPublicId());
    }

    @Transactional
    public SupportView assign(Long actorId, String requestId, String organizationId) {
        var actor = activeUser(actorId);
        var organization = activeOrganization(organizationId);
        var request = requests.lockByPublicId(requestId).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        if (!request.getUserId().equals(actor.getId())) throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        if (request.getOrganizationId() != null) {
            if (!request.getOrganizationId().equals(organization.getId()))
                throw ErrorCode.SUPPORT_ORGANIZATION_CONFLICT.toException();
            return view(request, organization.getPublicId());
        }
        request.assignOrganization(organization.getId());
        audits.save(new AuditEvent(actor.getId(), "SUPPORT_REQUEST_ROUTED", "support_request", request.getId(),
                sha256(request.getPublicId() + ":" + organization.getPublicId())));
        return view(request, organization.getPublicId());
    }

    private User activeUser(Long actorId) {
        if (actorId == null) throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        return users.lockForAuthorization(actorId).filter(candidate -> candidate.isActive())
                .orElseThrow(ErrorCode.AUTH_RESOURCE_ACCESS_DENIED::toException);
    }

    private Organization activeOrganization(String publicId) {
        if (publicId == null || publicId.isBlank()) throw ErrorCode.VALIDATION_ERROR.toException();
        return organizations.lockForAuthorizationByPublicId(publicId, Organization.OrgStatus.ACTIVE)
                .orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
    }

    private void requireOperationalRole(Long actorId, Long organizationId) {
        if (memberships.lockActiveForAuthorization(actorId, organizationId, List.of(UserRole.SUPPORT, UserRole.SYSTEM_ADMIN),
                Membership.MembershipStatus.ACTIVE, User.UserStatus.ACTIVE).isEmpty())
            throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
    }

    private static SupportView view(SupportRequest request, String organizationId) {
        return new SupportView(request.getPublicId(), request.getSubject(), request.getStatus().name(),
                request.getMessage(), organizationId);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String canonicalKey(String value) {
        if (value == null) return null;
        try {
            String canonical = UUID.fromString(value).toString();
            if (!canonical.equalsIgnoreCase(value)) throw new IllegalArgumentException();
            return canonical;
        } catch (IllegalArgumentException exception) {
            throw ErrorCode.VALIDATION_ERROR.toException();
        }
    }

    private static String fingerprint(String subject, String message, String organizationId) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            String presence = organizationId == null ? "0" : "1";
            String destination = organizationId == null ? "" : organizationId.toLowerCase(Locale.ROOT);
            for (String value : new String[]{"support-create-v1", subject, message, presence, destination}) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }

    public record SupportView(String id, String subject, String status, String message, String organizationId) {
        public SupportView { Objects.requireNonNull(id, "id"); }
    }
}
