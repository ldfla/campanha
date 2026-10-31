package la.meleva.fundraiser.ranking;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RankingCalculatorTest {

    @Test
    void rankingIsDeterministicAcrossInputOrderAndRecalculatesAfterInvalidation() {
        Instant early = Instant.parse("2026-01-01T00:00:00Z");
        Instant late = early.plusSeconds(1);
        var a = RankingCalculator.EligibleFact.of(2L, "user-2", early);
        var b = RankingCalculator.EligibleFact.of(1L, "user-1", early);
        var c = RankingCalculator.EligibleFact.of(2L, "user-2", late);

        var ranking = RankingCalculator.calculate(List.of(a, b, c));

        assertThat(ranking).isEqualTo(RankingCalculator.calculate(List.of(c, b, a)));
        assertThat(ranking).extracting(RankingCalculator.Position::userId).containsExactly(2L, 1L);
        assertThat(RankingCalculator.calculate(List.of(a, b)))
                .extracting(RankingCalculator.Position::userId).containsExactly(1L, 2L);
    }

    @Test
    void equalScoreAndEqualInstantFallBackToOpaquePublicId() {
        Instant instant = Instant.parse("2026-02-02T12:00:00Z");
        var first = RankingCalculator.EligibleFact.of(9L, "aaa", instant);
        var second = RankingCalculator.EligibleFact.of(7L, "bbb", instant);

        assertThat(RankingCalculator.calculate(List.of(second, first)))
                .extracting(RankingCalculator.Position::publicId)
                .containsExactly("aaa", "bbb");
    }

    @Test
    void positionsAreSequentialAndStartAtOne() {
        Instant instant = Instant.parse("2026-03-03T00:00:00Z");
        List<RankingCalculator.Position> ranking = RankingCalculator.calculate(List.of(
                RankingCalculator.EligibleFact.of(1L, "a", instant),
                RankingCalculator.EligibleFact.of(2L, "b", instant),
                RankingCalculator.EligibleFact.of(2L, "b", instant)));

        assertThat(ranking).extracting(RankingCalculator.Position::position).containsExactly(1, 2);
        assertThat(ranking.get(0).score()).isEqualTo(2);
        assertThat(ranking.get(1).score()).isEqualTo(1);
    }

    @Test
    void aggregatedFactUsesWeightAndKeepsFirstEligibleInstant() {
        Instant first = Instant.parse("2026-04-04T00:00:00Z");
        Instant later = first.plusSeconds(30);

        var ranking = RankingCalculator.calculate(List.of(
                new RankingCalculator.EligibleFact(1L, "a", later, 2),
                new RankingCalculator.EligibleFact(1L, "a", first, 1)));

        assertThat(ranking).hasSize(1);
        assertThat(ranking.get(0).score()).isEqualTo(3);
        assertThat(ranking.get(0).firstEligibleAt()).isEqualTo(first);
    }

    @Test
    void rejectsIncompleteFact() {
        assertThatThrownBy(() -> RankingCalculator.EligibleFact.of(1L, " ", Instant.EPOCH))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RankingCalculator.EligibleFact.of(1L, "a", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RankingCalculator.EligibleFact(1L, "a", Instant.EPOCH, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
