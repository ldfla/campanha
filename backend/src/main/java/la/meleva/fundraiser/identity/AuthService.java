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

/**
 * Autenticação por posse do WhatsApp: cadastro e login apenas solicitam um código; a sessão só
 * nasce após a verificação do código ({@link #verifyLoginCode}). As respostas de solicitação são
 * idênticas para telefones cadastrados ou não (sem enumeração).
 */
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

    /** Resultado da verificação do primeiro fator. */
    public sealed interface LoginOutcome {
        record Authenticated(UserResponse user) implements LoginOutcome { }
        record MfaRequired(Long userId, String phoneNumber) implements LoginOutcome { }
    }

    /** Cadastra (se inédito) e envia código; telefone já cadastrado recebe a mesma resposta. */
    @Transactional
    public void register(RegisterRequest request) {
        String normalizedPhone = normalizePhone(request.phoneNumber());
        throttleCodeDelivery(normalizedPhone);
        if (userRepository.existsByPhoneNumber(normalizedPhone)) {
            contactVerificationProvider.sendVerification(normalizedPhone);
            return;
        }
        var user = userRepository.save(new User(request.firstName(), request.surname(), normalizedPhone));
        contactVerificationProvider.sendVerification(normalizedPhone);
        outbox.save(new OutboxEvent("USER", user.getId(), "NOTIFICATION_ACCOUNT_REGISTERED_V1", "{}"));
    }

    /** Solicita código de acesso; não revela se o telefone existe. */
    @Transactional(readOnly = true)
    public void requestLoginCode(LoginRequest request) {
        String normalizedPhone = normalizePhone(request.phoneNumber());
        throttleCodeDelivery(normalizedPhone);
        userRepository.findByPhoneNumber(normalizedPhone)
                .filter(User::isActive)
                .ifPresent(user -> contactVerificationProvider.sendVerification(normalizedPhone));
    }

    /** Verifica o código de posse do telefone; administradores ainda precisam do segundo fator. */
    @Transactional
    public LoginOutcome verifyLoginCode(String phoneNumber, String code) {
        String normalizedPhone = normalizePhone(phoneNumber);
        String attemptKey = "otp:" + normalizedPhone;
        loginAttemptTracker.checkAllowed(attemptKey);
        var user = userRepository.findByPhoneNumber(normalizedPhone).orElse(null);
        if (user == null || !contactVerificationProvider.verify(normalizedPhone, code)) {
            loginAttemptTracker.recordFailure(attemptKey);
            throw ErrorCode.AUTH_INVALID_CREDENTIALS.toException();
        }
        loginAttemptTracker.recordSuccess(attemptKey);
        loginAttemptTracker.recordSuccess(sendKey(normalizedPhone));
        if (!user.isActive()) {
            throw ErrorCode.AUTH_ACCOUNT_LOCKED.toException();
        }
        user.markPhoneVerified();
        userRepository.save(user);

        var details = buildUserDetails(user);
        if (mfaService.ensureEnrollment(user, details.roles())) {
            return new LoginOutcome.MfaRequired(user.getId(), normalizedPhone);
        }
        setAuthentication(details);
        return new LoginOutcome.Authenticated(UserResponse.from(user, roleNames(details)));
    }

    @Transactional
    public void verifyContact(String phoneNumber, String code) {
        String normalizedPhone = normalizePhone(phoneNumber);
        String attemptKey = "otp:" + normalizedPhone;
        loginAttemptTracker.checkAllowed(attemptKey);
        var user = userRepository.findByPhoneNumber(normalizedPhone).orElse(null);
        if (user == null || !contactVerificationProvider.verify(normalizedPhone, code)) {
            loginAttemptTracker.recordFailure(attemptKey);
            throw ErrorCode.AUTH_INVALID_CREDENTIALS.toException();
        }
        loginAttemptTracker.recordSuccess(attemptKey);
        user.markPhoneVerified();
        userRepository.save(user);
    }

    /**
     * Segundo fator vinculado ao primeiro: {@code pendingUserId} vem da sessão criada por
     * {@link #verifyLoginCode}; sem ele, nenhum código é aceito.
     */
    @Transactional
    public UserResponse verifyMfa(Long pendingUserId, String phoneNumber, String code) {
        if (pendingUserId == null) throw ErrorCode.AUTH_MFA_REQUIRED.toException();
        String attemptKey = "mfa:" + pendingUserId;
        loginAttemptTracker.checkAllowed(attemptKey);
        var user = userRepository.findById(pendingUserId).filter(User::isActive)
                .orElseThrow(ErrorCode.AUTH_INVALID_CREDENTIALS::toException);
        if (!user.getPhoneNumber().equals(normalizePhone(phoneNumber))) {
            loginAttemptTracker.recordFailure(attemptKey);
            throw ErrorCode.AUTH_INVALID_CREDENTIALS.toException();
        }
        var details = buildUserDetails(user);
        try {
            mfaService.verify(user.getId(), code);
        } catch (ErrorCode.BusinessException exception) {
            loginAttemptTracker.recordFailure(attemptKey);
            throw exception;
        }
        loginAttemptTracker.recordSuccess(attemptKey);
        setAuthentication(details);
        return UserResponse.from(user, roleNames(details));
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(AppUserDetails details) {
        var user = userRepository.findById(details.userId())
                .orElseThrow(ErrorCode.AUTH_INVALID_CREDENTIALS::toException);
        return UserResponse.from(user, roleNames(details));
    }

    /** Limita envios por telefone (custo de SMS/WhatsApp e abuso), sem distinguir cadastro. */
    private void throttleCodeDelivery(String normalizedPhone) {
        String key = sendKey(normalizedPhone);
        loginAttemptTracker.checkAllowed(key);
        loginAttemptTracker.recordFailure(key);
    }

    private static String sendKey(String normalizedPhone) { return "send:" + normalizedPhone; }

    private static List<String> roleNames(AppUserDetails details) {
        return details.roles().stream().map(UserRole::name).toList();
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
