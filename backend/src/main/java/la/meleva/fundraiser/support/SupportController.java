package la.meleva.fundraiser.support;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import la.meleva.fundraiser.identity.AppUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/support/requests")
public class SupportController {

    private final SupportService service;

    SupportController(SupportService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public SupportService.SupportView open(@AuthenticationPrincipal AppUserDetails principal,
                                           @Valid @RequestBody OpenRequest request) {
        return service.open(principal.userId(), request.subject(), request.message());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPPORT', 'SYSTEM_ADMIN')")
    public List<SupportService.SupportView> list(@AuthenticationPrincipal AppUserDetails principal) {
        return service.listOpen(principal.userId());
    }

    public record OpenRequest(@NotBlank @Size(max = 150) String subject,
                              @NotBlank @Size(max = 2000) String message) { }
}
