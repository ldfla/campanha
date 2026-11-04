package la.meleva.fundraiser.payment;

import jakarta.validation.Validation;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinancialScenarioRequestTest {
    @Test
    void jsonDoesNotCoerceFractionalVersions() {
        var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        String payload = "{\"name\":\"Plano\",\"version\":1.5,\"scenario\":null}";
        assertThatThrownBy(() -> mapper.readValue(payload, FinancialScenarioController.UpdateRequest.class))
                .isInstanceOf(Exception.class);
    }

    @Test
    void validatesScenarioFieldsAndVersionThroughNestedRecords() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var input = new FinancialSimulationController.SimulationRequest(-1L, 0L, 100L, 5L,
                    0L, 0L, 0L, List.of());
            var request = new FinancialScenarioController.UpdateRequest("Plano", -1L, input);
            assertThat(validator.validate(request)).extracting(violation -> violation.getPropertyPath().toString())
                    .containsExactlyInAnyOrder("version", "scenario.priceCents");
            var blank = new FinancialScenarioController.CreateRequest(" ", input);
            assertThat(validator.validate(blank)).extracting(violation -> violation.getPropertyPath().toString())
                    .containsExactlyInAnyOrder("name", "scenario.priceCents");
        }
    }
}
