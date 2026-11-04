package la.meleva.fundraiser.payment;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinancialScenarioTest {
    @Test
    void updateReplacesCostsAndPreservesScenarioIdentity() {
        var initial = new FinancialSimulation.Scenario(100, 30, 100, 20, 100, 200, 500,
                List.of(new FinancialSimulation.FixedCost(FinancialSimulation.CostCategory.PRIZE, 1000)));
        var scenario = new FinancialScenario(1L, "Inicial", initial);
        var id = scenario.publicId();
        var updated = new FinancialSimulation.Scenario(120, 40, 150, 30, 150, 250, 700,
                List.of(new FinancialSimulation.FixedCost(FinancialSimulation.CostCategory.PLATFORM, 200)));
        scenario.update(" Atualizado ", updated);
        assertThat(scenario.publicId()).isEqualTo(id);
        assertThat(scenario.name()).isEqualTo("Atualizado");
        assertThat(scenario.scenario()).isEqualTo(updated);
        assertThatThrownBy(() -> scenario.update("  ", updated)).isInstanceOf(IllegalArgumentException.class);
    }
}
