package la.meleva.fundraiser.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import la.meleva.fundraiser.identity.dto.LoginRequest;
import la.meleva.fundraiser.identity.dto.RegisterRequest;
import la.meleva.fundraiser.identity.dto.UserResponse;
import la.meleva.fundraiser.identity.dto.VerifyContactRequest;
import la.meleva.fundraiser.identity.dto.VerifyMfaRequest;
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

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        var response = authService.register(request);
        rotateExistingSession(httpRequest);
        persistSecurityContext(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        var response = authService.login(request);
        rotateExistingSession(httpRequest);
        persistSecurityContext(httpRequest);
        return ResponseEntity.ok(response);
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

    @PostMapping("/mfa/verify")
    public ResponseEntity<UserResponse> verifyMfa(@Valid @RequestBody VerifyMfaRequest request, HttpServletRequest httpRequest) {
        var response = authService.verifyMfa(request.phoneNumber(), request.code());
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
