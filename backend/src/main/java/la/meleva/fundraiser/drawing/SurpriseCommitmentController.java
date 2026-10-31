package la.meleva.fundraiser.drawing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import la.meleva.fundraiser.identity.AppUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/surprises/campaigns/{campaignId}")
public class SurpriseCommitmentController {
    private final SurpriseCommitmentService service;
    public SurpriseCommitmentController(SurpriseCommitmentService service){this.service=service;}

    @GetMapping("/commitment") @PreAuthorize("permitAll()")
    public SurpriseCommitmentService.PublishedCommitment published(@PathVariable String campaignId){return service.published(campaignId);}

    @PostMapping("/enable") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAnyRole('CAMPAIGN_MANAGER','SYSTEM_ADMIN')")
    public void enable(@AuthenticationPrincipal AppUserDetails principal,@PathVariable String campaignId){service.enable(principal.userId(),campaignId);}

    @PostMapping("/commitment") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('CAMPAIGN_MANAGER','SYSTEM_ADMIN')")
    public SurpriseCommitmentService.PublishedCommitment commit(@AuthenticationPrincipal AppUserDetails principal,@PathVariable String campaignId,@Valid @RequestBody CreateRequest request){return service.commit(principal.userId(),campaignId,request.ruleVersion());}

    @PostMapping("/reveal") @PreAuthorize("hasAnyRole('DRAW_AUDITOR','SYSTEM_ADMIN')")
    public SurpriseProof.Secret reveal(@AuthenticationPrincipal AppUserDetails principal,@PathVariable String campaignId){return service.reveal(principal.userId(),campaignId);}

    @PostMapping("/reveal-approvals") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAnyRole('DRAW_AUDITOR','COMPLIANCE_REVIEWER')")
    public void approve(@AuthenticationPrincipal AppUserDetails principal,@PathVariable String campaignId,@Valid @RequestBody ApprovalRequest request){service.approveReveal(principal.userId(),campaignId,request.role());}

    @PostMapping("/verify") @PreAuthorize("isAuthenticated()")
    public VerifyResult verify(@PathVariable String campaignId,@Valid @RequestBody VerifyRequest request){
        return new VerifyResult(service.verify(campaignId,request.tier1(),request.tier2(),request.saltHex()));
    }

    public record CreateRequest(@Positive int ruleVersion) {}
    public record ApprovalRequest(@NotNull SurpriseRevealApproval.ApprovalRole role) {}
    public record VerifyRequest(@NotBlank String saltHex,
                                @NotNull @Size(min=SurpriseProof.TIER1_COUNT,max=SurpriseProof.TIER1_COUNT) List<@NotNull Integer> tier1,
                                @NotNull @Size(min=SurpriseProof.TIER2_COUNT,max=SurpriseProof.TIER2_COUNT) List<@NotNull Integer> tier2) {}
    public record VerifyResult(boolean valid) {}
}
