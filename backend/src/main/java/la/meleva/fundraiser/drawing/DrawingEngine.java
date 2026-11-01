package la.meleva.fundraiser.drawing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Motor puro de apuração: recebe a regra, o resultado oficial e o conjunto congelado de
 * participações elegíveis, e devolve os vencedores por posição. Sem efeitos colaterais e sem
 * acesso a banco — a mesma entrada produz sempre a mesma saída.
 */
public final class DrawingEngine {

    public static final String MATCH = "MATCH";
    public static final String NO_WINNER = "NO_WINNER";
    public static final String ABSENT = "ABSENT";

    private DrawingEngine() {}

    public static List<Resolved> compute(FederalDrawRule rule, List<String> federalPrizes, List<FrozenEntry> frozen) {
        Objects.requireNonNull(rule, "rule");
        Objects.requireNonNull(federalPrizes, "federalPrizes");
        Objects.requireNonNull(frozen, "frozen");
        Map<Integer, FrozenEntry> byNumber = new HashMap<>();
        for (FrozenEntry entry : frozen) {
            byNumber.putIfAbsent(entry.numberValue(), entry);
        }
        List<Resolved> resolved = new ArrayList<>(rule.prizePositions().size());
        for (int position = 1; position <= rule.prizePositions().size(); position++) {
            int federalIndex = rule.prizePositions().get(position - 1);
            if (federalIndex > federalPrizes.size()) {
                resolved.add(new Resolved(position, null, null, ABSENT));
                continue;
            }
            Integer winning = toCampaignNumber(federalPrizes.get(federalIndex - 1), rule.digits());
            if (winning == null) {
                resolved.add(new Resolved(position, null, null, ABSENT));
                continue;
            }
            FrozenEntry owner = byNumber.get(winning);
            resolved.add(new Resolved(position, winning, owner == null ? null : owner.entryId(),
                    owner == null ? NO_WINNER : MATCH));
        }
        return List.copyOf(resolved);
    }

    /** Últimos {@code digits} dígitos do bilhete, em 0–99999 (o {@code 00000} é preservado). */
    static Integer toCampaignNumber(String rawBilhete, int digits) {
        if (rawBilhete == null) return null;
        String digitsOnly = rawBilhete.replaceAll("\\D", "");
        if (digitsOnly.isEmpty()) return null;
        long value;
        try {
            value = Long.parseLong(digitsOnly);
        } catch (NumberFormatException exception) {
            return null;
        }
        long modulus = 1;
        for (int index = 0; index < digits; index++) modulus *= 10;
        return (int) (value % modulus);
    }

    public record FrozenEntry(long entryId, int numberValue) {}

    public record Resolved(int position, Integer winningNumber, Long entryId, String decisionCode) {}
}
