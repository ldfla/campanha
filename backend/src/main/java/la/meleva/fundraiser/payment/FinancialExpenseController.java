package la.meleva.fundraiser.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import la.meleva.fundraiser.identity.AppUserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.annotation.JsonDeserialize;

@RestController
@RequestMapping("/financial/campaigns/{campaignId}/expenses")
class FinancialExpenseController {
    private final FinancialExpenseService service;
    private final FinancialExpenseReversalService reversals;
    FinancialExpenseController(FinancialExpenseService service, FinancialExpenseReversalService reversals) {
        this.service = service;
        this.reversals = reversals;
    }

    @PostMapping("/{expenseId}/reversal")
    @PreAuthorize("hasAnyRole('FINANCE', 'SYSTEM_ADMIN')")
    FinancialExpenseReversalService.View reverse(@AuthenticationPrincipal AppUserDetails principal,
            @PathVariable String campaignId, @PathVariable String expenseId, @Valid @RequestBody ReversalRequest request) {
        return reversals.reverse(principal.userId(), campaignId, expenseId, request.idempotencyKey(), request.reason());
    }

    @GetMapping("/{expenseId}/reversal")
    @PreAuthorize("hasAnyRole('FINANCE', 'SYSTEM_ADMIN')")
    FinancialExpenseReversalService.View reversal(@AuthenticationPrincipal AppUserDetails principal,
            @PathVariable String campaignId, @PathVariable String expenseId) {
        return reversals.get(principal.userId(), campaignId, expenseId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('FINANCE', 'SYSTEM_ADMIN')")
    FinancialExpenseService.View create(@AuthenticationPrincipal AppUserDetails principal,
                                        @PathVariable String campaignId, @Valid @RequestBody CreateRequest request) {
        return service.create(principal.userId(), campaignId, request.idempotencyKey(), request.category(),
                request.amountCents(), request.description(), request.occurredAt());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FINANCE', 'SYSTEM_ADMIN')")
    List<FinancialExpenseService.View> list(@AuthenticationPrincipal AppUserDetails principal,
                                            @PathVariable String campaignId,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return service.list(principal.userId(), campaignId, page, size);
    }

    record CreateRequest(@NotBlank @Size(min = 36, max = 36) String idempotencyKey,
                         @NotNull FinancialSimulation.CostCategory category,
                         @NotNull @Positive @JsonDeserialize(using = FinancialSimulationController.StrictLong.class) Long amountCents,
                         @NotBlank @Size(max = 200) String description,
                         @NotNull Instant occurredAt) { }
    record ReversalRequest(@NotBlank @Size(min = 36, max = 36) String idempotencyKey,
                           @NotBlank @Size(max = 200) String reason) { }
}
