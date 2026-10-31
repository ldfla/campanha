package la.meleva.fundraiser.ranking.dto;

import la.meleva.fundraiser.ranking.RankingMetric;
import la.meleva.fundraiser.ranking.RankingScope;
import java.time.Instant;
import java.util.List;

/**
 * Placar público pseudonimizado. Nunca carrega identificador interno, nome completo ou telefone.
 */
public record RankingView(
        String campaignPublicId,
        String campaignTitle,
        RankingScope scope,
        RankingMetric metric,
        int ruleVersion,
        String tieBreaker,
        Instant cutoffAt,
        Instant generatedAt,
        String consistency,
        boolean simulation,
        boolean awardsEligible,
        String notice,
        List<Row> entries) {

    public record Row(int position, String pseudonym, long score, Instant firstEventAt) {}
}
