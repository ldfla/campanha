package la.meleva.fundraiser.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import la.meleva.fundraiser.identity.dto.CodeChallengeResponse;
import la.meleva.fundraiser.identity.dto.LoginRequest;
import la.meleva.fundraiser.identity.dto.RegisterRequest;
import la.meleva.fundraiser.identity.dto.UserResponse;
import la.meleva.fundraiser.identity.dto.VerifyContactRequest;
import la.meleva.fundraiser.identity.dto.VerifyLoginRequest;
import la.meleva.fundraiser.identity.dto.VerifyMfaRequest;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    static final String PENDING_MFA_ATTRIBUTE = "auth.pendingMfa";
    private static final Duration PENDING_MFA_TTL = Duration.ofMinutes(5);

    /** Primeiro fator concluído aguardando MFA; vive somente na sessão do servidor. */
    record PendingMfa(Long userId, Instant expiresAt) implements Serializable { }

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Cadastro não cria sessão: a posse do telefone é comprovada em {@code /auth/login/verify}. */
    @PostMapping("/register")
    public ResponseEntity<CodeChallengeResponse> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(CodeChallengeResponse.sent());
    }

    /** Solicita código de acesso; a resposta é a mesma para telefones cadastrados ou não. */
    @PostMapping("/login")
    public ResponseEntity<CodeChallengeResponse> login(@Valid @RequestBody LoginRequest request) {
        authService.requestLoginCode(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(CodeChallengeResponse.sent());
    }

    @PostMapping("/login/verify")
    public ResponseEntity<UserResponse> verifyLogin(@Valid @RequestBody VerifyLoginRequest request,
                                                    HttpServletRequest httpRequest) {
        var outcome = authService.verifyLoginCode(request.phoneNumber(), request.code());
        rotateExistingSession(httpRequest);
        return switch (outcome) {
            case AuthService.LoginOutcome.Authenticated authenticated -> {
                persistSecurityContext(httpRequest);
                yield ResponseEntity.ok(authenticated.user());
            }
            case AuthService.LoginOutcome.MfaRequired pending -> {
                HttpSession session = httpRequest.getSession(true);
                session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
                session.setAttribute(PENDING_MFA_ATTRIBUTE,
                        new PendingMfa(pending.userId(), Instant.now().plus(PENDING_MFA_TTL)));
                throw ErrorCode.AUTH_MFA_REQUIRED.toException();
            }
        };
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-contact")
    public ResponseEntity<Void> verifyContact(@Valid @RequestBody VerifyContactRequest request) {
        authService.verifyContact(request.phoneNumber(), request.code());
        return ResponseEntity.noContent().build();
    }

    /** O segundo fator só é aceito na mesma sessão em que o primeiro foi verificado. */
    @PostMapping("/mfa/verify")
    public ResponseEntity<UserResponse> verifyMfa(@Valid @RequestBody VerifyMfaRequest request, HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        Long pendingUserId = null;
        if (session != null && session.getAttribute(PENDING_MFA_ATTRIBUTE) instanceof PendingMfa pending
                && Instant.now().isBefore(pending.expiresAt())) {
            pendingUserId = pending.userId();
        }
        var response = authService.verifyMfa(pendingUserId, request.phoneNumber(), request.code());
        if (session != null) session.removeAttribute(PENDING_MFA_ATTRIBUTE);
        rotateExistingSession(httpRequest);
        persistSecurityContext(httpRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal AppUserDetails details) {
        if (details == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(authService.getCurrentUser(details));
    }

    private void persistSecurityContext(HttpServletRequest request) {
        SecurityContext context = SecurityContextHolder.getContext();
        HttpSession session = request.getSession(true);
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context);
    }

    private void rotateExistingSession(HttpServletRequest request) {
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
    }
}
