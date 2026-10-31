package la.meleva.fundraiser.drawing;

import la.meleva.fundraiser.identity.AppUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Importação e revisão em dupla do resultado oficial da Loteria Federal. */
@RestController
@RequestMapping("/drawings/federal-results")
public class FederalResultController {

    private final FederalResultService service;

    FederalResultController(FederalResultService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('DRAW_AUDITOR', 'SYSTEM_ADMIN')")
    public FederalResultService.ResultView importResult(@AuthenticationPrincipal AppUserDetails principal,
                                                        @RequestParam int contest) {
        return service.importResult(principal.userId(), contest);
    }

    @PostMapping("/{federalResultId}/reviews")
    public FederalResultService.ResultView review(@AuthenticationPrincipal AppUserDetails principal,
                                                  @PathVariable Long federalResultId) {
        return service.review(principal.userId(), federalResultId);
    }

    @GetMapping("/{contestIdentifier}")
    @PreAuthorize("hasAnyRole('DRAW_AUDITOR', 'SYSTEM_ADMIN')")
    public FederalResultService.ResultView get(@PathVariable String contestIdentifier) {
        return service.get(contestIdentifier);
    }
}
