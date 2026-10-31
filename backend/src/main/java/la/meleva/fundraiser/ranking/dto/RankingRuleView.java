package la.meleva.fundraiser.ranking.dto;

import la.meleva.fundraiser.ranking.RankingMetric;
import la.meleva.fundraiser.ranking.RankingScope;
import java.time.Instant;

public record RankingRuleView(
        int ruleVersion,
        RankingScope scope,
        RankingMetric metric,
        String tieBreaker,
        boolean legallyApproved,
        Instant effectiveAt) {}
