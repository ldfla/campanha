package la.meleva.fundraiser.privacy;

import la.meleva.fundraiser.identity.AppUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Direitos do titular sobre os próprios dados (LGPD). */
@RestController
@RequestMapping("/privacy/me")
public class PrivacyController {

    private final PrivacyService service;

    PrivacyController(PrivacyService service) {
        this.service = service;
    }

    @GetMapping("/export")
    @PreAuthorize("isAuthenticated()")
    public PrivacyService.ExportView export(@AuthenticationPrincipal AppUserDetails principal) {
        return service.export(principal.userId());
    }

    @PostMapping("/anonymize")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void anonymize(@AuthenticationPrincipal AppUserDetails principal) {
        service.anonymize(principal.userId());
    }
}
