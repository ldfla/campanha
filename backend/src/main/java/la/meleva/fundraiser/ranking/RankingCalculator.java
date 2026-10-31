package la.meleva.fundraiser.ranking;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agrega fatos elegíveis e aplica o desempate fixo do servidor:
 * pontuação desc, instante do primeiro fato elegível asc e identificador público opaco asc.
 *
 * <p>Função pura, sem acesso a banco, para que a regra de ordenação seja testável isoladamente
 * e permaneça idêntica entre placar vivo e snapshot.</p>
 */
public final class RankingCalculator {

    private static final Comparator<Aggregate> ORDER = Comparator
            .comparingLong(Aggregate::score).reversed()
            .thenComparing(Aggregate::firstEligibleAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Aggregate::publicId);

    private RankingCalculator() {}

    /**
     * @param facts um fato por evento ou uma linha agregada (com {@code weight} > 1)
     */
    public static List<Position> calculate(List<EligibleFact> facts) {
        Map<Long, Aggregate> totals = new LinkedHashMap<>();
        for (EligibleFact fact : facts) {
            totals.compute(fact.userId(), (id, current) -> current == null
                    ? new Aggregate(fact.userId(), fact.publicId(), fact.weight(), fact.occurredAt())
                    : new Aggregate(fact.userId(), current.publicId(),
                            Math.addExact(current.score(), fact.weight()),
                            current.firstEligibleAt().isBefore(fact.occurredAt())
                                    ? current.firstEligibleAt()
                                    : fact.occurredAt()));
        }
        List<Aggregate> ordered = totals.values().stream().sorted(ORDER).toList();
        List<Position> positions = new ArrayList<>(ordered.size());
        for (int index = 0; index < ordered.size(); index++) {
            Aggregate aggregate = ordered.get(index);
            positions.add(new Position(index + 1, aggregate.userId(), aggregate.publicId(),
                    aggregate.score(), aggregate.firstEligibleAt()));
        }
        return List.copyOf(positions);
    }

    public record EligibleFact(long userId, String publicId, Instant occurredAt, long weight) {

        public EligibleFact {
            if (publicId == null || publicId.isBlank()) {
                throw new IllegalArgumentException("Fato elegível sem identificador público");
            }
            if (occurredAt == null) {
                throw new IllegalArgumentException("Fato elegível sem instante");
            }
            if (weight <= 0) {
                throw new IllegalArgumentException("Peso do fato elegível deve ser positivo");
            }
        }

        /** Fato unitário, usado por testes e por agregações evento a evento. */
        public static EligibleFact of(long userId, String publicId, Instant occurredAt) {
            return new EligibleFact(userId, publicId, occurredAt, 1);
        }
    }

    public record Position(int position, long userId, String publicId, long score, Instant firstEligibleAt) {}

    private record Aggregate(long userId, String publicId, long score, Instant firstEligibleAt) {}
}
