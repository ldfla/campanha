package la.meleva.fundraiser.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import la.meleva.fundraiser.audit.AuditEvent;
import la.meleva.fundraiser.audit.AuditEventRepository;
import la.meleva.fundraiser.identity.MembershipRepository;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.identity.UserRole;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Suporte interno. A listagem é restrita a papéis operacionais e não expõe dados pessoais do autor. */
@Service
public class SupportService {

    private static final int MAX_PAGE = 100;

    private final SupportRequestRepository requests;
    private final UserRepository users;
    private final MembershipRepository memberships;
    private final AuditEventRepository audits;

    SupportService(SupportRequestRepository requests, UserRepository users, MembershipRepository memberships,
                   AuditEventRepository audits) {
        this.requests = requests;
        this.users = users;
        this.memberships = memberships;
        this.audits = audits;
    }

    @Transactional
    public SupportView open(Long userId, String subject, String message) {
        if (userId == null) throw ErrorCode.VALIDATION_ERROR.toException();
        var user = users.findById(userId).filter(candidate -> candidate.isActive())
                .orElseThrow(ErrorCode.AUTH_RESOURCE_ACCESS_DENIED::toException);
        if (subject == null || subject.isBlank() || subject.strip().length() > 150
                || message == null || message.isBlank() || message.strip().length() > 2000) {
            throw ErrorCode.VALIDATION_ERROR.toException();
        }
        var saved = requests.save(new SupportRequest(user.getId(), subject.strip(), message.strip()));
        audits.save(new AuditEvent(user.getId(), "SUPPORT_REQUEST_OPENED", "support_request", saved.getId(),
                sha256(saved.getPublicId())));
        return view(saved);
    }

    @Transactional(readOnly = true)
    public List<SupportView> listOpen(Long actorId) {
        requireOperationalRole(actorId);
        return requests.findByStatusOrderByIdAsc(SupportRequest.SupportStatus.OPEN, PageRequest.of(0, MAX_PAGE))
                .stream().map(SupportService::view).toList();
    }

    private void requireOperationalRole(Long actorId) {
        if (actorId == null) throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        var user = users.findById(actorId).filter(candidate -> candidate.isActive())
                .orElseThrow(ErrorCode.AUTH_RESOURCE_ACCESS_DENIED::toException);
        boolean allowed = memberships.findActiveByUserId(user.getId()).stream()
                .anyMatch(membership -> membership.getRole() == UserRole.SUPPORT
                        || membership.getRole() == UserRole.SYSTEM_ADMIN);
        if (!allowed) throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
    }

    private static SupportView view(SupportRequest request) {
        return new SupportView(request.getPublicId(), request.getSubject(), request.getStatus().name(),
                request.getMessage());
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public record SupportView(String id, String subject, String status, String message) {
        public SupportView { Objects.requireNonNull(id, "id"); }
    }
}
