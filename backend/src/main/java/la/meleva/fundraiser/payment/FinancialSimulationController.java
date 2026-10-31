package la.meleva.fundraiser.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import la.meleva.fundraiser.identity.AppUserDetails;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

@RestController
@RequestMapping("/financial/campaigns/{campaignId}")
public class FinancialSimulationController {
    private final FinancialSimulationService service;

    FinancialSimulationController(FinancialSimulationService service) {
        this.service = service;
    }

    @PostMapping("/simulations")
    @PreAuthorize("hasAnyRole('FINANCE', 'CAMPAIGN_MANAGER', 'SYSTEM_ADMIN')")
    public SimulationResponse calculate(@AuthenticationPrincipal AppUserDetails principal,
                                        @PathVariable String campaignId,
                                        @Valid @RequestBody SimulationRequest request) {
        if (request.quantity() > request.capacity() || request.feeBasisPoints() + request.taxBasisPoints() > 10_000) {
            throw ErrorCode.VALIDATION_ERROR.toException();
        }
        var costs = request.fixedCosts().stream()
                .map(cost -> new FinancialSimulation.FixedCost(cost.category(), cost.cents())).toList();
        var scenario = new FinancialSimulation.Scenario(request.priceCents(), request.unitCostCents(),
                request.capacity().intValue(), request.quantity().intValue(), request.feeBasisPoints().intValue(), request.taxBasisPoints().intValue(),
                request.targetProfitCents(), costs);
        var result = service.calculate(principal.userId(), campaignId, scenario);
        return new SimulationResponse(result.currency(), result.hypothetical(), result.grossCents(),
                result.unitCostsCents(), result.fixedCostsCents(), result.feesCents(), result.taxesCents(),
                result.profitCents(), result.breakEvenQuantity().isPresent() ? result.breakEvenQuantity().getAsLong() : null,
                result.targetQuantity().isPresent() ? result.targetQuantity().getAsLong() : null,
                "SIMULAÇÃO HIPOTÉTICA: não representa caixa real nem autorização de operação.");
    }

    public record FixedCostRequest(@NotNull FinancialSimulation.CostCategory category,
                                   @NotNull @PositiveOrZero @JsonDeserialize(using = StrictLong.class) Long cents) { }

    public record SimulationRequest(@NotNull @PositiveOrZero @JsonDeserialize(using = StrictLong.class) Long priceCents,
                                    @NotNull @PositiveOrZero @JsonDeserialize(using = StrictLong.class) Long unitCostCents,
                                    @NotNull @Min(0) @Max(100_000) @JsonDeserialize(using = StrictLong.class) Long capacity,
                                    @NotNull @Min(0) @Max(100_000) @JsonDeserialize(using = StrictLong.class) Long quantity,
                                    @NotNull @Min(0) @Max(10_000) @JsonDeserialize(using = StrictLong.class) Long feeBasisPoints,
                                    @NotNull @Min(0) @Max(10_000) @JsonDeserialize(using = StrictLong.class) Long taxBasisPoints,
                                    @NotNull @PositiveOrZero @JsonDeserialize(using = StrictLong.class) Long targetProfitCents,
                                    @NotNull @Size(max = 100) List<@NotNull @Valid FixedCostRequest> fixedCosts) { }

    public static final class StrictLong extends ValueDeserializer<Long> {
        @Override
        public Long deserialize(JsonParser parser, DeserializationContext context) {
            if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                return (Long) context.handleUnexpectedToken(Long.class, parser);
            }
            return parser.getLongValue();
        }
    }

    public record SimulationResponse(String currency, boolean hypothetical, long grossCents, long unitCostsCents,
                                     long fixedCostsCents, long feesCents, long taxesCents, long profitCents,
                                     Long breakEvenQuantity, Long targetQuantity, String notice) { }
}
