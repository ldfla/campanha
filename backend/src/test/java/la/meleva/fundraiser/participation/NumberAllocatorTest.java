package la.meleva.fundraiser.participation;

import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/** Sorteio uniforme de candidatos do {@link NumberAllocator} (puro, sem banco). */
class NumberAllocatorTest {

    @Test
    void drawsDistinctValuesInsideRange() {
        var random = new Random(42);
        Set<Integer> drawn = NumberAllocator.distinctCandidates(random, 0, 100_000, 5, Set.of());
        assertThat(drawn).hasSize(5).doesNotHaveDuplicates();
        assertThat(drawn).allMatch(value -> value >= 0 && value <= 99_999);
    }

    @Test
    void isUniformRatherThanTheLowestBlock() {
        // Em várias sementes o sorteio não pode ser sempre o bloco mais baixo {0,1,2,3,4}.
        boolean sawNonLowest = false;
        for (long seed = 0; seed < 20; seed++) {
            Set<Integer> drawn = NumberAllocator.distinctCandidates(new Random(seed), 0, 100_000, 5, Set.of());
            if (!drawn.equals(Set.of(0, 1, 2, 3, 4))) sawNonLowest = true;
        }
        assertThat(sawNonLowest).isTrue();
    }

    @Test
    void neverRepeatsExcludedValues() {
        var random = new Random(7);
        Set<Integer> excluded = Set.of(1, 2, 3, 4, 5);
        Set<Integer> drawn = NumberAllocator.distinctCandidates(random, 0, 10, 4, excluded);
        assertThat(drawn).hasSize(4).doesNotContainAnyElementsOf(excluded);
    }

    @Test
    void stopsWhenRangeIsExhausted() {
        var random = new Random(1);
        // Só há 3 valores no intervalo; pedindo 5, retorna no máximo 3 e não entra em laço infinito.
        Set<Integer> drawn = NumberAllocator.distinctCandidates(random, 0, 3, 5, Set.of());
        assertThat(drawn.size()).isBetween(1, 3);
        assertThat(drawn).allMatch(value -> value >= 0 && value <= 2);
    }
}
