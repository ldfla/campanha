package la.meleva.fundraiser.notification;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import la.meleva.fundraiser.identity.AppUserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Preferências de notificação do próprio usuário; comunicação essencial é inextinguível. */
@RestController
@RequestMapping("/notifications/preferences")
public class NotificationPreferenceController {

    private final NotificationPreferenceService service;

    NotificationPreferenceController(NotificationPreferenceService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public NotificationPreferenceService.PreferenceView view(@AuthenticationPrincipal AppUserDetails principal) {
        return service.view(principal.userId());
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public NotificationPreferenceService.PreferenceView update(@AuthenticationPrincipal AppUserDetails principal,
                                                               @Valid @RequestBody PreferenceRequest request) {
        return service.update(principal.userId(), request.marketingOptOut());
    }

    public record PreferenceRequest(@NotNull Boolean marketingOptOut) { }
}
