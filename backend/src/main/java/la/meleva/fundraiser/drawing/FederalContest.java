package la.meleva.fundraiser.drawing;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Resultado de um concurso da Loteria Federal lido de uma fonte oficial.
 * {@code prizes} preserva os bilhetes como publicados (ex.: "069015").
 */
public record FederalContest(int contestNumber, LocalDate drawnOn, List<String> prizes,
                             String rawPayload, String sourceUrl) {
    public FederalContest {
        Objects.requireNonNull(drawnOn, "drawnOn");
        prizes = List.copyOf(prizes);
        Objects.requireNonNull(rawPayload, "rawPayload");
        Objects.requireNonNull(sourceUrl, "sourceUrl");
    }
}
