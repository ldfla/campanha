package la.meleva.fundraiser.drawing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import la.meleva.fundraiser.identity.AppUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Regra, congelamento, apuração e publicação (P12). A transparência é pública. */
@RestController
@RequestMapping("/drawings/campaigns/{campaignId}")
public class DrawingController {

    private final DrawingService service;

    DrawingController(DrawingService service) {
        this.service = service;
    }

    @PostMapping("/rules")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CAMPAIGN_MANAGER', 'SYSTEM_ADMIN')")
    public DrawingService.RuleView registerRule(@AuthenticationPrincipal AppUserDetails principal,
                                                @PathVariable String campaignId,
                                                @Valid @RequestBody RuleRequest request) {
        return service.registerRule(principal.userId(), campaignId, request.formulaJson());
    }

    @PostMapping("/draws")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CAMPAIGN_MANAGER', 'SYSTEM_ADMIN')")
    public DrawingService.DrawView freeze(@AuthenticationPrincipal AppUserDetails principal,
                                          @PathVariable String campaignId) {
        return service.freeze(principal.userId(), campaignId);
    }

    @PostMapping("/draws/{drawPublicId}/compute")
    @PreAuthorize("hasAnyRole('DRAW_AUDITOR', 'SYSTEM_ADMIN')")
    public DrawingService.DrawView compute(@AuthenticationPrincipal AppUserDetails principal,
                                           @PathVariable String campaignId,
                                           @PathVariable String drawPublicId,
                                           @Valid @RequestBody ComputeRequest request) {
        return service.compute(principal.userId(), campaignId, drawPublicId, request.contestIdentifier());
    }

    @PostMapping("/draws/{drawPublicId}/publish")
    @PreAuthorize("hasAnyRole('DRAW_AUDITOR', 'SYSTEM_ADMIN')")
    public DrawingService.DrawView publish(@AuthenticationPrincipal AppUserDetails principal,
                                           @PathVariable String campaignId,
                                           @PathVariable String drawPublicId) {
        return service.publish(principal.userId(), campaignId, drawPublicId);
    }

    @GetMapping("/draws/{drawPublicId}/transparency")
    public DrawingService.TransparencyView transparency(@PathVariable String campaignId,
                                                        @PathVariable String drawPublicId) {
        return service.transparency(campaignId, drawPublicId);
    }

    public record RuleRequest(@NotBlank String formulaJson) { }

    public record ComputeRequest(@NotBlank String contestIdentifier) { }
}
