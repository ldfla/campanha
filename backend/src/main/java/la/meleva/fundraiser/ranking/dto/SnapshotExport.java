package la.meleva.fundraiser.ranking.dto;

import la.meleva.fundraiser.ranking.RankingScope;
import java.time.Instant;

public record SnapshotExport(
        String snapshotId,
        String campaignPublicId,
        RankingScope scope,
        Instant cutoffAt,
        Instant generatedAt,
        String snapshotHash,
        boolean verified,
        String payloadJson) {}
