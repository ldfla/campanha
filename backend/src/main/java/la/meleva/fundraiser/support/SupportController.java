package la.meleva.fundraiser.support;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import la.meleva.fundraiser.identity.AppUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
                                           @Valid @RequestBody OpenRequest request,
                                           @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return service.open(principal.userId(), request.subject(), request.message(), request.organizationId(), idempotencyKey);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<SupportService.SupportView> list(@AuthenticationPrincipal AppUserDetails principal,
                                                  @RequestParam(required = false) String organizationId,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "100") int size,
                                                  @RequestParam(defaultValue = "OPEN") SupportRequest.SupportStatus status) {
        return service.list(principal.userId(), organizationId, page, size, status);
    }

    @PatchMapping("/{requestId}/status")
    @PreAuthorize("hasAnyRole('SUPPORT', 'SYSTEM_ADMIN')")
    public SupportService.SupportView transition(@AuthenticationPrincipal AppUserDetails principal,
            @PathVariable String requestId, @Valid @RequestBody StatusRequest request) {
        return service.transition(principal.userId(), requestId, request.status(), request.revision());
    }

    @PostMapping("/{requestId}/organization")
    @PreAuthorize("isAuthenticated()")
    public SupportService.SupportView assign(@AuthenticationPrincipal AppUserDetails principal,
            @PathVariable String requestId, @Valid @RequestBody OrganizationRequest request) {
        return service.assign(principal.userId(), requestId, request.organizationId());
    }

    public record OpenRequest(@NotBlank @Size(max = 150) String subject,
                              @NotBlank @Size(max = 2000) String message,
                              @Size(max = 36) String organizationId) { }
    public record OrganizationRequest(@NotBlank @Size(max = 36) String organizationId) { }
    public record StatusRequest(@NotNull SupportRequest.SupportStatus status, @Min(0) Long revision) { }
}
