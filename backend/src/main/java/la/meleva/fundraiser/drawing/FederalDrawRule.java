package la.meleva.fundraiser.drawing;

import java.util.List;
import java.util.Objects;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Regra declarativa de apuração da Federal. **A fórmula não é improvisada**: os parâmetros
 * (`digits` e a ordem dos prêmios da Federal) vêm do regulamento aprovado (ver D-OPEN-008).
 *
 * <p>JSON esperado: {@code {"source":"FEDERAL","digits":5,"prizePositions":[1,2,3,4,5]}} — a
 * posição i do ranking usa o prêmio {@code prizePositions[i-1]} da Federal, tomando os {@code digits}
 * últimos dígitos do bilhete.</p>
 */
public record FederalDrawRule(int digits, List<Integer> prizePositions) {

    public static final String SOURCE = "FEDERAL";

    public FederalDrawRule {
        if (digits < 1 || digits > 5) throw new IllegalArgumentException("draw.rule.digits_invalid");
        Objects.requireNonNull(prizePositions, "prizePositions");
        prizePositions = List.copyOf(prizePositions);
        if (prizePositions.isEmpty() || prizePositions.stream().anyMatch(index -> index == null || index < 1 || index > 5)) {
            throw new IllegalArgumentException("draw.rule.positions_invalid");
        }
    }

    public static FederalDrawRule parse(String formulaJson) {
        JsonNode root = JsonMapper.builder().build().readTree(formulaJson);
        String source = root.path("source").asString("");
        if (!SOURCE.equalsIgnoreCase(source)) throw new IllegalArgumentException("draw.rule.source_invalid");
        int digits = root.path("digits").asInt(5);
        var positions = new java.util.ArrayList<Integer>();
        for (JsonNode node : root.path("prizePositions")) positions.add(node.asInt());
        return new FederalDrawRule(digits, positions);
    }
}
