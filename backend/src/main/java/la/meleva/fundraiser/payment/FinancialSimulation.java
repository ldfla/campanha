package la.meleva.fundraiser.payment;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;

/** Cálculo hipotético em centavos; não representa caixa nem aprovação operacional. */
public final class FinancialSimulation {
    private static final BigInteger BASIS_POINTS = BigInteger.valueOf(10_000);
    private static final int MAX_CAPACITY = 100_000;

    private FinancialSimulation() { }

    public enum CostCategory {
        PRIZE, COMMISSION, PLATFORM, COMMUNICATION, OTHER
    }

    public record FixedCost(CostCategory category, long cents) {
        public FixedCost {
            Objects.requireNonNull(category, "category");
            if (cents < 0) throw new IllegalArgumentException("Custo negativo");
        }
    }

    public record Scenario(long priceCents, long unitCostCents, int capacity, int quantity,
                           int feeBasisPoints, int taxBasisPoints, long targetProfitCents,
                           List<FixedCost> fixedCosts) {
        public Scenario {
            if (priceCents < 0 || unitCostCents < 0 || targetProfitCents < 0
                    || capacity < 0 || capacity > MAX_CAPACITY || quantity < 0 || quantity > capacity
                    || feeBasisPoints < 0 || feeBasisPoints > 10_000
                    || taxBasisPoints < 0 || taxBasisPoints > 10_000
                    || feeBasisPoints + taxBasisPoints > 10_000) {
                throw new IllegalArgumentException("Parâmetros inválidos para simulação");
            }
            fixedCosts = List.copyOf(fixedCosts);
        }
    }

    public record Result(String currency, boolean hypothetical, long grossCents, long unitCostsCents,
                         long fixedCostsCents, long feesCents, long taxesCents, long profitCents,
                         OptionalLong breakEvenQuantity, OptionalLong targetQuantity) { }

    public static Result calculate(Scenario scenario) {
        Objects.requireNonNull(scenario, "scenario");
        long fixed = 0;
        for (FixedCost cost : scenario.fixedCosts()) fixed = Math.addExact(fixed, cost.cents());
        Totals actual = totals(scenario, scenario.quantity(), fixed);
        OptionalLong breakEven = OptionalLong.empty();
        OptionalLong target = OptionalLong.empty();
        // Arredondamentos independentes podem tornar a margem não monotônica.
        for (int quantity = 0; quantity <= scenario.capacity(); quantity++) {
            BigInteger profit = projectedProfit(scenario, quantity, fixed);
            if (breakEven.isEmpty() && profit.signum() >= 0) breakEven = OptionalLong.of(quantity);
            if (target.isEmpty() && profit.compareTo(BigInteger.valueOf(scenario.targetProfitCents())) >= 0) {
                target = OptionalLong.of(quantity);
            }
            if (breakEven.isPresent() && target.isPresent()) break;
        }
        return new Result("BRL", true, actual.gross(), actual.unitCosts(), fixed, actual.fees(),
                actual.taxes(), actual.profit(), breakEven, target);
    }

    private static Totals totals(Scenario scenario, int quantity, long fixed) {
        long gross = Math.multiplyExact(scenario.priceCents(), quantity);
        long unitCosts = Math.multiplyExact(scenario.unitCostCents(), quantity);
        long fees = percentage(gross, scenario.feeBasisPoints());
        long taxes = percentage(gross, scenario.taxBasisPoints());
        long profit = Math.subtractExact(Math.subtractExact(Math.subtractExact(
                Math.subtractExact(gross, unitCosts), fixed), fees), taxes);
        return new Totals(gross, unitCosts, fees, taxes, profit);
    }

    private static long percentage(long gross, int basisPoints) {
        return percentage(BigInteger.valueOf(gross), basisPoints).longValueExact();
    }

    private static BigInteger percentage(BigInteger gross, int basisPoints) {
        return gross.multiply(BigInteger.valueOf(basisPoints)).add(BigInteger.valueOf(5_000)).divide(BASIS_POINTS);
    }

    private static BigInteger projectedProfit(Scenario scenario, int quantity, long fixed) {
        BigInteger units = BigInteger.valueOf(quantity);
        BigInteger gross = BigInteger.valueOf(scenario.priceCents()).multiply(units);
        return gross.subtract(BigInteger.valueOf(scenario.unitCostCents()).multiply(units))
                .subtract(BigInteger.valueOf(fixed))
                .subtract(percentage(gross, scenario.feeBasisPoints()))
                .subtract(percentage(gross, scenario.taxBasisPoints()));
    }

    private record Totals(long gross, long unitCosts, long fees, long taxes, long profit) { }
}
