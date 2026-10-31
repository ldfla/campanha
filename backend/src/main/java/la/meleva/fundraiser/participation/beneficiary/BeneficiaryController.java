package la.meleva.fundraiser.participation.beneficiary;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import la.meleva.fundraiser.identity.AppUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/participations/campaigns/{campaignId}/beneficiaries")
public class BeneficiaryController {
    private final BeneficiaryService service;

    BeneficiaryController(BeneficiaryService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<BeneficiaryService.BeneficiaryView> list(@PathVariable String campaignId) {
        return service.list(campaignId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CAMPAIGN_MANAGER', 'SYSTEM_ADMIN')")
    public BeneficiaryService.BeneficiaryView create(@AuthenticationPrincipal AppUserDetails principal,
                                                     @PathVariable String campaignId,
                                                     @Valid @RequestBody CreateBeneficiaryRequest request) {
        return service.create(principal.userId(), campaignId, request.name());
    }

    public record CreateBeneficiaryRequest(@NotBlank @Size(max = 150) String name) { }
}
