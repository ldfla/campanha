package la.meleva.fundraiser.ranking;

import jakarta.validation.Valid;
import java.util.List;
import la.meleva.fundraiser.identity.AppUserDetails;
import la.meleva.fundraiser.ranking.dto.CreateSnapshotRequest;
import la.meleva.fundraiser.ranking.dto.RankingRuleRequest;
import la.meleva.fundraiser.ranking.dto.RankingRuleView;
import la.meleva.fundraiser.ranking.dto.RankingView;
import la.meleva.fundraiser.ranking.dto.SnapshotExport;
import la.meleva.fundraiser.ranking.dto.SnapshotView;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rankings/campaigns/{campaignId}")
public class RankingController {

    private final RankingService service;

    RankingController(RankingService service) {
        this.service = service;
    }

    @GetMapping("/{scope}")
    public RankingView current(@PathVariable String campaignId, @PathVariable RankingScope scope,
                               @RequestParam(defaultValue = "50") int limit) {
        return service.current(campaignId, scope, limit);
    }

    @PostMapping("/rules")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CAMPAIGN_MANAGER', 'SYSTEM_ADMIN')")
    public RankingRuleView registerRule(@AuthenticationPrincipal AppUserDetails principal,
                                        @PathVariable String campaignId,
                                        @Valid @RequestBody RankingRuleRequest request) {
        return service.registerRule(principal.userId(), campaignId, request);
    }

    @PostMapping("/snapshots")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CAMPAIGN_MANAGER', 'DRAW_AUDITOR', 'SYSTEM_ADMIN')")
    public SnapshotView createSnapshot(@AuthenticationPrincipal AppUserDetails principal,
                                       @PathVariable String campaignId,
                                       @Valid @RequestBody CreateSnapshotRequest request) {
        return service.snapshot(principal.userId(), campaignId, request);
    }

    @GetMapping("/snapshots")
    public List<SnapshotView> snapshots(@PathVariable String campaignId,
                                        @RequestParam RankingScope scope) {
        return service.listSnapshots(campaignId, scope);
    }

    @GetMapping("/snapshots/{snapshotId}")
    public SnapshotView snapshot(@PathVariable String campaignId, @PathVariable String snapshotId) {
        return service.snapshot(campaignId, snapshotId);
    }

    @GetMapping("/snapshots/{snapshotId}/export")
    public SnapshotExport export(@AuthenticationPrincipal AppUserDetails principal,
                                 @PathVariable String campaignId, @PathVariable String snapshotId) {
        return service.export(principal == null ? null : principal.userId(), campaignId, snapshotId);
    }
}
