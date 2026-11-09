package la.meleva.fundraiser.payment;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinancialExpenseTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void acceptsOnlyPositiveCostsWithExistingTimeAndMatchesExactReplay() {
        var entry = new FinancialExpense(1L, 2L, "2512abae-f1e9-448b-a06b-6bc859694a5b",
                FinancialSimulation.CostCategory.PLATFORM, 123, " Plataforma ", NOW, NOW);
        assertThat(entry.matches(2L, FinancialSimulation.CostCategory.PLATFORM, 123, "Plataforma", NOW)).isTrue();
        assertThat(entry.matches(3L, FinancialSimulation.CostCategory.PLATFORM, 123, "Plataforma", NOW)).isFalse();
        assertThat(entry.matches(2L, FinancialSimulation.CostCategory.PLATFORM, 124, "Plataforma", NOW)).isFalse();
        assertThatThrownBy(() -> new FinancialExpense(1L, 2L, "key", FinancialSimulation.CostCategory.PLATFORM,
                0, "Plataforma", NOW, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FinancialExpense(1L, 2L, "key", FinancialSimulation.CostCategory.PLATFORM,
                1, " ", NOW, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FinancialExpense(1L, 2L, "key", FinancialSimulation.CostCategory.PLATFORM,
                1, "Plataforma", NOW.plusSeconds(1), NOW)).isInstanceOf(IllegalArgumentException.class);
    }
}
