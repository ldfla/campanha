package la.meleva.fundraiser.ranking.dto;

import jakarta.validation.constraints.NotNull;
import la.meleva.fundraiser.ranking.RankingMetric;
import la.meleva.fundraiser.ranking.RankingScope;
import java.time.Instant;

public record RankingRuleRequest(
        @NotNull RankingScope scope,
        @NotNull RankingMetric metric,
        Instant effectiveAt) {}
