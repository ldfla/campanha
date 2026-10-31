package la.meleva.fundraiser.participation;

import jakarta.validation.Valid;
import la.meleva.fundraiser.identity.AppUserDetails;
import la.meleva.fundraiser.participation.dto.CreateParticipationRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/participations/campaigns/{campaignId}")
public class ParticipationController {
    private final ParticipationService service;

    ParticipationController(ParticipationService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ParticipationAllocation> request(@AuthenticationPrincipal AppUserDetails principal,
            @PathVariable String campaignId, @Valid @RequestBody CreateParticipationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.request(principal.userId(), campaignId, request.idempotencyKey(), request.count()));
    }

    @GetMapping("/availability")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Long> availability(@PathVariable String campaignId) {
        return ResponseEntity.ok(service.availableCount(campaignId));
    }

    @GetMapping("/numbers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ParticipationService.NumberAvailability>> numbers(@PathVariable String campaignId,
            @PageableDefault(size = 100, sort = "numberValue") Pageable pageable) {
        return ResponseEntity.ok(service.numbers(campaignId, pageable));
    }

    @PostMapping("/freeze")
    @PreAuthorize("hasAnyRole('CAMPAIGN_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<Integer> freeze(@PathVariable String campaignId) {
        return ResponseEntity.ok(service.freezeAssignedNumbers(campaignId));
    }
}
