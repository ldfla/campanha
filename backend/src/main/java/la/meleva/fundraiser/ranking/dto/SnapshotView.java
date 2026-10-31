package la.meleva.fundraiser.ranking.dto;

import la.meleva.fundraiser.ranking.RankingMetric;
import la.meleva.fundraiser.ranking.RankingScope;
import java.time.Instant;

public record SnapshotView(
        String snapshotId,
        String campaignPublicId,
        RankingScope scope,
        RankingMetric metric,
        int ruleVersion,
        Instant cutoffAt,
        Instant generatedAt,
        String snapshotHash,
        int entryCount) {}
