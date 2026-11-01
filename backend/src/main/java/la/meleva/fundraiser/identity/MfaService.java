package la.meleva.fundraiser.identity;

import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** MFA sandbox: o segredo é armazenado como bytes; o verificador é injetado e só existe em perfis DEMO. */
@Service
public class MfaService {
    private final MfaEnrollmentRepository enrollmentRepository;
    private final MfaCodeVerifier verifier;

    MfaService(MfaEnrollmentRepository enrollmentRepository, MfaCodeVerifier verifier) {
        this.enrollmentRepository = enrollmentRepository;
        this.verifier = verifier;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean ensureEnrollment(User user, List<UserRole> roles) {
        if (roles.stream().noneMatch(this::isAdministrative)) return false;
        enrollmentRepository.findByUserId(user.getId())
                .orElseGet(() -> enrollmentRepository.save(new MfaEnrollment(user.getId(), "demo-totp-secret".getBytes(StandardCharsets.UTF_8))));
        return true;
    }

    @Transactional
    public void verify(Long userId, String code) {
        var enrollment = enrollmentRepository.findByUserId(userId)
                .orElseThrow(ErrorCode.AUTH_MFA_REQUIRED::toException);
        if (!verifier.verify(enrollment, code)) throw ErrorCode.AUTH_MFA_REQUIRED.toException();
        if (!enrollment.isVerified()) { enrollment.verify(); enrollmentRepository.save(enrollment); }
    }

    private boolean isAdministrative(UserRole role) {
        return role == UserRole.SYSTEM_ADMIN || role == UserRole.SECURITY_ADMIN;
    }
}
