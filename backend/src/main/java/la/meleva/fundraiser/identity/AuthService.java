package la.meleva.fundraiser.identity;

import la.meleva.fundraiser.identity.dto.LoginRequest;
import la.meleva.fundraiser.identity.dto.RegisterRequest;
import la.meleva.fundraiser.identity.dto.UserResponse;
import la.meleva.fundraiser.shared.ErrorCode;
import la.meleva.fundraiser.notification.OutboxEvent;
import la.meleva.fundraiser.notification.OutboxEventRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final ContactVerificationProvider contactVerificationProvider;
    private final MfaService mfaService;
    private final LoginAttemptTracker loginAttemptTracker;
    private final OutboxEventRepository outbox;

    public AuthService(UserRepository userRepository, MembershipRepository membershipRepository,
                       ContactVerificationProvider contactVerificationProvider, MfaService mfaService,
                       LoginAttemptTracker loginAttemptTracker, OutboxEventRepository outbox) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.contactVerificationProvider = contactVerificationProvider;
        this.mfaService = mfaService;
        this.loginAttemptTracker = loginAttemptTracker;
        this.outbox = outbox;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String normalizedPhone = normalizePhone(request.phoneNumber());

        if (userRepository.existsByPhoneNumber(normalizedPhone)) {
            throw ErrorCode.AUTH_DUPLICATE_CREDENTIAL.toException();
        }

        var user = new User(request.firstName(), request.surname(), normalizedPhone);
        user = userRepository.save(user);
        contactVerificationProvider.sendVerification(normalizedPhone);
        outbox.save(new OutboxEvent("USER", user.getId(), "NOTIFICATION_ACCOUNT_REGISTERED_V1", "{}"));

        var details = buildUserDetails(user);
        setAuthentication(details);

        return UserResponse.from(user, List.of(UserRole.PARTICIPANT.name()));
    }

    @Transactional
    public void verifyContact(String phoneNumber, String code) {
        String normalizedPhone = normalizePhone(phoneNumber);
        var user = userRepository.findByPhoneNumber(normalizedPhone)
                .orElseThrow(ErrorCode.AUTH_INVALID_CREDENTIALS::toException);
        if (!contactVerificationProvider.verify(normalizedPhone, code)) {
            throw ErrorCode.AUTH_INVALID_CREDENTIALS.toException();
        }
        user.markPhoneVerified();
        userRepository.save(user);
    }

    @Transactional
    public UserResponse verifyMfa(String phoneNumber, String code) {
        String normalizedPhone = normalizePhone(phoneNumber);
        var user = userRepository.findByPhoneNumber(normalizedPhone)
                .orElseThrow(ErrorCode.AUTH_INVALID_CREDENTIALS::toException);
        var details = buildUserDetails(user);
        mfaService.verify(user.getId(), code);
        setAuthentication(details);
        return UserResponse.from(user, details.roles().stream().map(UserRole::name).toList());
    }

    @Transactional
    public UserResponse login(LoginRequest request) {
        String normalizedPhone = normalizePhone(request.phoneNumber());
        loginAttemptTracker.checkAllowed(normalizedPhone);

        var user = userRepository.findByPhoneNumber(normalizedPhone).orElse(null);
        if (user == null) {
            loginAttemptTracker.recordFailure(normalizedPhone);
            throw ErrorCode.AUTH_INVALID_CREDENTIALS.toException();
        }

        if (!user.isActive()) {
            throw ErrorCode.AUTH_ACCOUNT_LOCKED.toException();
        }

        var details = buildUserDetails(user);
        if (mfaService.ensureEnrollment(user, details.roles())) {
            throw ErrorCode.AUTH_MFA_REQUIRED.toException();
        }
        setAuthentication(details);
        loginAttemptTracker.recordSuccess(normalizedPhone);

        var roleNames = details.roles().stream().map(UserRole::name).toList();
        return UserResponse.from(user, roleNames);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(AppUserDetails details) {
        var user = userRepository.findById(details.userId())
                .orElseThrow(ErrorCode.AUTH_INVALID_CREDENTIALS::toException);

        var roleNames = details.roles().stream().map(UserRole::name).toList();
        return UserResponse.from(user, roleNames);
    }

    private AppUserDetails buildUserDetails(User user) {
        var memberships = membershipRepository.findActiveByUserId(user.getId());
        var roles = memberships.stream()
                .map(Membership::getRole)
                .distinct()
                .toList();

        List<UserRole> effectiveRoles = roles.isEmpty()
                ? List.of(UserRole.PARTICIPANT)
                : roles;

        return new AppUserDetails(
                user.getId(),
                user.getPublicId(),
                user.getPhoneNumber(),
                effectiveRoles
        );
    }

    private void setAuthentication(AppUserDetails details) {
        var auth = new UsernamePasswordAuthenticationToken(
                details, null, details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private String normalizePhone(String phone) {
        try {
            return la.meleva.fundraiser.shared.PhoneNumbers.toE164(phone);
        } catch (RuntimeException exception) {
            throw ErrorCode.VALIDATION_ERROR.toException();
        }
    }
}
