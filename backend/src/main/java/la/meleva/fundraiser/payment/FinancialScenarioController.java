package la.meleva.fundraiser.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import la.meleva.fundraiser.identity.AppUserDetails;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.annotation.JsonDeserialize;

@RestController
@RequestMapping("/financial/campaigns/{campaignId}/scenarios")
class FinancialScenarioController {
    private final FinancialScenarioService service;
    FinancialScenarioController(FinancialScenarioService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAnyRole('FINANCE', 'CAMPAIGN_MANAGER', 'SYSTEM_ADMIN')")
    FinancialScenarioService.View create(@AuthenticationPrincipal AppUserDetails principal,
                                         @PathVariable String campaignId, @Valid @RequestBody CreateRequest request) {
        return service.create(principal.userId(), campaignId, request.name(), scenario(request.scenario()));
    }

    @PutMapping("/{scenarioId}")
    @PreAuthorize("hasAnyRole('FINANCE', 'CAMPAIGN_MANAGER', 'SYSTEM_ADMIN')")
    FinancialScenarioService.View update(@AuthenticationPrincipal AppUserDetails principal,
                                         @PathVariable String campaignId, @PathVariable String scenarioId,
                                         @Valid @RequestBody UpdateRequest request) {
        return service.update(principal.userId(), campaignId, scenarioId, request.version(), request.name(),
                scenario(request.scenario()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FINANCE', 'CAMPAIGN_MANAGER', 'SYSTEM_ADMIN')")
    List<FinancialScenarioService.View> list(@AuthenticationPrincipal AppUserDetails principal,
                                            @PathVariable String campaignId,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) throw ErrorCode.VALIDATION_ERROR.toException();
        return service.list(principal.userId(), campaignId, page, size);
    }

    private FinancialSimulation.Scenario scenario(FinancialSimulationController.SimulationRequest request) {
        try {
            var costs = request.fixedCosts().stream()
                    .map(cost -> new FinancialSimulation.FixedCost(cost.category(), cost.cents())).toList();
            return new FinancialSimulation.Scenario(request.priceCents(), request.unitCostCents(),
                    request.capacity().intValue(), request.quantity().intValue(), request.feeBasisPoints().intValue(),
                    request.taxBasisPoints().intValue(), request.targetProfitCents(), costs);
        } catch (IllegalArgumentException ex) { throw ErrorCode.VALIDATION_ERROR.toException(); }
    }

    record CreateRequest(@NotBlank @Size(max = 120) String name,
                         @NotNull @Valid FinancialSimulationController.SimulationRequest scenario) { }
    record UpdateRequest(@NotBlank @Size(max = 120) String name,
                         @NotNull @PositiveOrZero @JsonDeserialize(using = FinancialSimulationController.StrictLong.class) Long version,
                         @NotNull @Valid FinancialSimulationController.SimulationRequest scenario) { }
}
