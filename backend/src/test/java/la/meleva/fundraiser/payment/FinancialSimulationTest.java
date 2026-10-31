package la.meleva.fundraiser.payment;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class FinancialSimulationTest {
    @Test
    void calculatesHypotheticalProfitAndTargetsInCents() {
        var scenario = new FinancialSimulation.Scenario(100, 56, 100, 100, 1_000, 0, 2_000,
                List.of(new FinancialSimulation.FixedCost(FinancialSimulation.CostCategory.PRIZE, 1_000)));
        var result = FinancialSimulation.calculate(scenario);
        assertEquals("BRL", result.currency());
        assertTrue(result.hypothetical());
        assertEquals(10_000, result.grossCents());
        assertEquals(5_600, result.unitCostsCents());
        assertEquals(1_000, result.feesCents());
        assertEquals(2_400, result.profitCents());
        assertEquals(30, result.breakEvenQuantity().orElseThrow());
        assertEquals(89, result.targetQuantity().orElseThrow());
    }

    @Test
    void roundsEachRateHalfUpAndSearchesEvenWhenProfitIsNotMonotonic() {
        var scenario = new FinancialSimulation.Scenario(1, 0, 4, 1, 5_000, 5_000, 0, List.of());
        var result = FinancialSimulation.calculate(scenario);
        assertEquals(1, result.feesCents());
        assertEquals(1, result.taxesCents());
        assertEquals(-1, result.profitCents());
        assertEquals(0, result.breakEvenQuantity().orElseThrow());
        var withFixedCost = new FinancialSimulation.Scenario(1, 0, 4, 1, 5_000, 5_000, 0,
                List.of(new FinancialSimulation.FixedCost(FinancialSimulation.CostCategory.OTHER, 1)));
        assertTrue(FinancialSimulation.calculate(withFixedCost).breakEvenQuantity().isEmpty());
    }

    @Test
    void rejectsInvalidInputsAndOverflowWithoutWrapping() {
        assertThrows(IllegalArgumentException.class, () -> new FinancialSimulation.Scenario(1, 0, 1, 2, 0, 0, 0, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new FinancialSimulation.Scenario(1, 0, 1, 1, 6_000, 5_000, 0, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new FinancialSimulation.FixedCost(FinancialSimulation.CostCategory.OTHER, -1));
        assertThrows(ArithmeticException.class, () -> FinancialSimulation.calculate(
                new FinancialSimulation.Scenario(Long.MAX_VALUE, 0, 2, 2, 0, 0, 0, List.of())));
    }

    @Test
    void largePercentageCalculationDoesNotOverflowIntermediateProduct() {
        var result = FinancialSimulation.calculate(new FinancialSimulation.Scenario(
                Long.MAX_VALUE, 0, 1, 1, 10_000, 0, 0, List.of()));
        assertEquals(Long.MAX_VALUE, result.feesCents());
        assertEquals(0, result.profitCents());
    }

    @Test
    void futureProjectionDoesNotOverflowWhenCurrentTotalsFitInLong() {
        var scenario = new FinancialSimulation.Scenario(4_611_686_018_427_387_904L, 0, 2, 1, 0, 0,
                Long.MAX_VALUE, List.of());
        var result = FinancialSimulation.calculate(scenario);
        assertEquals(4_611_686_018_427_387_904L, result.grossCents());
        assertEquals(2, result.targetQuantity().orElseThrow());
        var deficit = new FinancialSimulation.Scenario(0, Long.MAX_VALUE, 2, 0, 0, 0, 0,
                List.of(new FinancialSimulation.FixedCost(FinancialSimulation.CostCategory.OTHER, Long.MAX_VALUE)));
        assertTrue(FinancialSimulation.calculate(deficit).breakEvenQuantity().isEmpty());
    }
}
