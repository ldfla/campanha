package la.meleva.fundraiser.drawing;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DrawingEngineTest {

    private static final List<String> CONCURSO_6027 = List.of("069015", "015794", "050550", "059017", "013549");

    @Test void mapsFederalPrizesToCampaignNumbersWithFixedVectors() {
        var rule = new FederalDrawRule(5, List.of(1, 2, 3, 4, 5));
        var frozen = List.of(
                new DrawingEngine.FrozenEntry(11L, 69015),
                new DrawingEngine.FrozenEntry(33L, 50550));

        var resolved = DrawingEngine.compute(rule, CONCURSO_6027, frozen);

        assertThat(resolved).containsExactly(
                new DrawingEngine.Resolved(1, 69015, 11L, DrawingEngine.MATCH),
                new DrawingEngine.Resolved(2, 15794, null, DrawingEngine.NO_WINNER),
                new DrawingEngine.Resolved(3, 50550, 33L, DrawingEngine.MATCH),
                new DrawingEngine.Resolved(4, 59017, null, DrawingEngine.NO_WINNER),
                new DrawingEngine.Resolved(5, 13549, null, DrawingEngine.NO_WINNER));
        // Mesma entrada → mesma saída.
        assertThat(DrawingEngine.compute(rule, CONCURSO_6027, frozen)).isEqualTo(resolved);
    }

    @Test void preservesZeroNumberAndSupportsDifferentDigitCounts() {
        var zero = new DrawingEngine.FrozenEntry(7L, 0);
        var fiveDigits = DrawingEngine.compute(new FederalDrawRule(5, List.of(1)),
                List.of("100000"), List.of(zero));
        assertThat(fiveDigits).containsExactly(new DrawingEngine.Resolved(1, 0, 7L, DrawingEngine.MATCH));

        var threeDigits = DrawingEngine.compute(new FederalDrawRule(3, List.of(1)),
                List.of("069015"), List.of(new DrawingEngine.FrozenEntry(9L, 15)));
        assertThat(threeDigits).containsExactly(new DrawingEngine.Resolved(1, 15, 9L, DrawingEngine.MATCH));
    }

    @Test void marksAbsentWhenFederalPrizeIsMissing() {
        var resolved = DrawingEngine.compute(new FederalDrawRule(5, List.of(1, 2)), List.of("069015"), List.of());
        assertThat(resolved).containsExactly(
                new DrawingEngine.Resolved(1, 69015, null, DrawingEngine.NO_WINNER),
                new DrawingEngine.Resolved(2, null, null, DrawingEngine.ABSENT));
    }

    @Test void rejectsInvalidRulesAndParsesDeclarativeJson() {
        assertThatThrownBy(() -> new FederalDrawRule(0, List.of(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FederalDrawRule(6, List.of(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FederalDrawRule(5, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FederalDrawRule(5, List.of(6))).isInstanceOf(IllegalArgumentException.class);

        var parsed = FederalDrawRule.parse("{\"source\":\"FEDERAL\",\"digits\":5,\"prizePositions\":[1,2,3,4,5]}");
        assertThat(parsed.digits()).isEqualTo(5);
        assertThat(parsed.prizePositions()).containsExactly(1, 2, 3, 4, 5);
        assertThatThrownBy(() -> FederalDrawRule.parse("{\"source\":\"OTHER\",\"digits\":5,\"prizePositions\":[1]}"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
