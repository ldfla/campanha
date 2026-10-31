package la.meleva.fundraiser.referral;

import la.meleva.fundraiser.identity.AppUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/referrals/campaigns/{campaignId}")
public class ReferralController {
    private final ReferralService service;
    ReferralController(ReferralService service) { this.service = service; }

    @PostMapping("/link")
    @PreAuthorize("hasAnyRole('PROMOTER', 'CAMPAIGN_MANAGER', 'SYSTEM_ADMIN')")
    ResponseEntity<ReferralService.ReferralMetrics> link(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long campaignId) {
        service.link(campaignId, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.metrics(campaignId, principal.userId()));
    }

    @PostMapping("/click/{code}")
    ResponseEntity<Void> click(@PathVariable Long campaignId, @PathVariable String code, @RequestParam String anonymousId) {
        service.click(campaignId, code, anonymousId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/attributions/{code}/{entryId}")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<Void> attribute(@PathVariable Long campaignId, @PathVariable String code, @PathVariable Long entryId) {
        service.attribute(campaignId, code, entryId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/metrics")
    @PreAuthorize("hasRole('PROMOTER')")
    ResponseEntity<ReferralService.ReferralMetrics> metrics(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long campaignId) {
        return ResponseEntity.ok(service.metrics(campaignId, principal.userId()));
    }
}
