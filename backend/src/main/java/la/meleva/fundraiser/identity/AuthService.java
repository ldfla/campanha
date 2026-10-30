package la.meleva.fundraiser.identity;

import la.meleva.fundraiser.identity.dto.LoginRequest;
import la.meleva.fundraiser.identity.dto.RegisterRequest;
import la.meleva.fundraiser.identity.dto.UserResponse;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    public AuthService(UserRepository userRepository, MembershipRepository membershipRepository) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String normalizedPhone = normalizePhone(request.phoneNumber());

        if (userRepository.existsByPhoneNumber(normalizedPhone)) {
            throw ErrorCode.AUTH_DUPLICATE_CREDENTIAL.toException();
        }

        var user = new User(request.firstName(), request.surname(), normalizedPhone);
        user = userRepository.save(user);

        var details = buildUserDetails(user);
        setAuthentication(details);

        return UserResponse.from(user, List.of(UserRole.PARTICIPANT.name()));
    }

    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest request) {
        String normalizedPhone = normalizePhone(request.phoneNumber());

        var user = userRepository.findByPhoneNumber(normalizedPhone)
                .orElseThrow(ErrorCode.AUTH_INVALID_CREDENTIALS::toException);

        if (!user.isActive()) {
            throw ErrorCode.AUTH_ACCOUNT_LOCKED.toException();
        }

        var details = buildUserDetails(user);
        setAuthentication(details);

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
        String digits = phone.replaceAll("[^\\d+]", "");
        if (!digits.startsWith("+")) {
            digits = "+55" + digits;
        }
        return digits;
    }
}
