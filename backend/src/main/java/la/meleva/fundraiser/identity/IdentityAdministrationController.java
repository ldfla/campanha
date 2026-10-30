package la.meleva.fundraiser.identity;

import jakarta.validation.Valid;
import la.meleva.fundraiser.identity.dto.CreateMembershipRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/memberships")
public class IdentityAdministrationController {
    private final IdentityAdministrationService service;
    IdentityAdministrationController(IdentityAdministrationService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Long> create(@AuthenticationPrincipal AppUserDetails principal, @Valid @RequestBody CreateMembershipRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addMembership(principal.userId(), request).getId());
    }

    @DeleteMapping("/{membershipId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Void> deactivate(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long membershipId) {
        service.deactivateMembership(principal.userId(), membershipId);
        return ResponseEntity.noContent().build();
    }
}
