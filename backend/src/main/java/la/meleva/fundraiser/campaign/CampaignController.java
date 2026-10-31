package la.meleva.fundraiser.campaign;

import jakarta.validation.Valid;
import la.meleva.fundraiser.campaign.dto.CampaignResponse;
import la.meleva.fundraiser.campaign.dto.CreateCampaignRequest;
import la.meleva.fundraiser.campaign.dto.PrizeRequest;
import la.meleva.fundraiser.campaign.dto.PrizeResponse;
import la.meleva.fundraiser.campaign.dto.TransitionRequest;
import la.meleva.fundraiser.campaign.dto.UpdateCampaignRequest;
import la.meleva.fundraiser.identity.AppUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CAMPAIGN_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<CampaignResponse> create(
            @Valid @RequestBody CreateCampaignRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {
        var campaign = campaignService.create(request, 1L);
        return ResponseEntity.status(HttpStatus.CREATED).body(CampaignResponse.from(campaign));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignResponse> getById(@PathVariable String id) {
        var campaign = campaignService.findByPublicId(id);
        return ResponseEntity.ok(CampaignResponse.from(campaign));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<CampaignResponse> getBySlug(@PathVariable String slug) {
        var campaign = campaignService.findBySlug(slug);
        return ResponseEntity.ok(CampaignResponse.from(campaign));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CAMPAIGN_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<CampaignResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateCampaignRequest request) {
        var campaign = campaignService.update(id, request);
        return ResponseEntity.ok(CampaignResponse.from(campaign));
    }

    @PostMapping("/{id}/transition")
    @PreAuthorize("hasAnyRole('CAMPAIGN_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<CampaignResponse> transition(
            @PathVariable String id,
            @Valid @RequestBody TransitionRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {
        var campaign = campaignService.transition(principal.userId(), id, request.targetStatus());
        return ResponseEntity.ok(CampaignResponse.from(campaign));
    }

    @PostMapping("/{id}/prizes")
    @PreAuthorize("hasAnyRole('CAMPAIGN_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<PrizeResponse> addPrize(
            @PathVariable String id,
            @Valid @RequestBody PrizeRequest request) {
        var prize = campaignService.addPrize(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(PrizeResponse.from(prize));
    }

    @GetMapping("/{id}/prizes")
    public ResponseEntity<List<PrizeResponse>> listPrizes(@PathVariable String id) {
        var prizes = campaignService.listPrizes(id).stream()
                .map(PrizeResponse::from)
                .toList();
        return ResponseEntity.ok(prizes);
    }

    @PutMapping("/{campaignId}/prizes/{prizeId}")
    @PreAuthorize("hasAnyRole('CAMPAIGN_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<PrizeResponse> updatePrize(
            @PathVariable String campaignId,
            @PathVariable Long prizeId,
            @Valid @RequestBody PrizeRequest request) {
        var prize = campaignService.updatePrize(prizeId, request);
        return ResponseEntity.ok(PrizeResponse.from(prize));
    }

    @DeleteMapping("/{campaignId}/prizes/{prizeId}")
    @PreAuthorize("hasAnyRole('CAMPAIGN_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<Void> deactivatePrize(
            @PathVariable String campaignId,
            @PathVariable Long prizeId) {
        campaignService.deactivatePrize(prizeId);
        return ResponseEntity.noContent().build();
    }
}
